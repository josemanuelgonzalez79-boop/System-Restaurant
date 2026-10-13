package com.rest.restaurantsystem.backup;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.rest.restaurantsystem.exception.ConflictException;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class BackupService {

    private static final Duration TOOL_TIMEOUT = Duration.ofMinutes(15);
    private static final DateTimeFormatter FILE_DATE = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")
            .withZone(ZoneOffset.UTC);
    private final Semaphore singleBackup = new Semaphore(1);
    private final PostgresToolRunner toolRunner;
    private final String databaseUrl;
    private final String username;
    private final String password;
    private final String pgDump;
    private final String pgRestore;

    public BackupService(PostgresToolRunner toolRunner,
                         @Value("${spring.datasource.url}") String databaseUrl,
                         @Value("${spring.datasource.username}") String username,
                         @Value("${spring.datasource.password}") String password,
                         @Value("${app.backup.pg-dump:pg_dump}") String pgDump,
                         @Value("${app.backup.pg-restore:pg_restore}") String pgRestore) {
        this.toolRunner = toolRunner;
        this.databaseUrl = databaseUrl;
        this.username = username;
        this.password = password;
        this.pgDump = pgDump;
        this.pgRestore = pgRestore;
    }

    public BackupArchive create() throws IOException, InterruptedException {
        if (!singleBackup.tryAcquire()) {
            throw new ConflictException("Ya se está generando un respaldo. Intenta de nuevo más tarde.");
        }
        Path dump = null;
        Path archive = null;
        boolean completed = false;
        try {
            if (!databaseUrl.startsWith("jdbc:postgresql://")) {
                throw new IllegalStateException("La URL de PostgreSQL no es apta para respaldo.");
            }
            URI database = URI.create(databaseUrl.substring("jdbc:".length()));
            if (database.getHost() == null
                    || database.getUserInfo() != null || database.getRawQuery() != null
                    || database.getPath() == null
                    || !database.getPath().matches("/[a-zA-Z_][a-zA-Z0-9_]*")) {
                throw new IllegalStateException("La URL de PostgreSQL no es apta para respaldo.");
            }
            String databaseName = database.getPath().substring(1);
            String connection = databaseUrl.substring("jdbc:".length());
            Instant createdAt = Instant.now();
            String name = "Essential-" + FILE_DATE.format(createdAt) + ".zip";
            dump = Files.createTempFile("essential-backup-", ".dump");
            archive = Files.createTempFile("essential-backup-", ".zip");

            toolRunner.run(pgDump, List.of("--dbname=" + connection, "--username=" + username,
                    "--no-password", "--format=custom", "--no-owner", "--no-acl",
                    "--file=" + dump.toAbsolutePath()), password, TOOL_TIMEOUT);
            if (Files.size(dump) == 0) {
                throw new IOException("PostgreSQL generó un archivo vacío.");
            }
            toolRunner.run(pgRestore, List.of("--list", dump.toAbsolutePath().toString()),
                    password, Duration.ofMinutes(2));
            String checksum = checksum(dump);
            String manifest = "{\n  \"format\": 1,\n  \"application\": \"Essential\",\n"
                    + "  \"database\": \"" + databaseName + "\",\n"
                    + "  \"createdAtUtc\": \"" + createdAt + "\",\n"
                    + "  \"dumpFile\": \"database.dump\",\n"
                    + "  \"sha256\": \"" + checksum + "\",\n"
                    + "  \"bytes\": " + Files.size(dump) + "\n}\n";
            try (OutputStream output = Files.newOutputStream(archive);
                 ZipOutputStream zip = new ZipOutputStream(output)) {
                zip.putNextEntry(new ZipEntry("manifest.json"));
                zip.write(manifest.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                zip.closeEntry();
                zip.putNextEntry(new ZipEntry("database.dump"));
                Files.copy(dump, zip);
                zip.closeEntry();
            }
            completed = true;
            return new BackupArchive(archive, name);
        } finally {
            try {
                try {
                    if (dump != null) Files.deleteIfExists(dump);
                } finally {
                    if (!completed && archive != null) Files.deleteIfExists(archive);
                }
            } finally {
                singleBackup.release();
            }
        }
    }

    private String checksum(Path dump) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (DigestInputStream input = new DigestInputStream(Files.newInputStream(dump), digest)) {
                input.transferTo(OutputStream.nullOutputStream());
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 no está disponible.", exception);
        }
    }

    public record BackupArchive(Path path, String filename) implements AutoCloseable {
        @Override
        public void close() throws IOException {
            Files.deleteIfExists(path);
        }
    }
}
