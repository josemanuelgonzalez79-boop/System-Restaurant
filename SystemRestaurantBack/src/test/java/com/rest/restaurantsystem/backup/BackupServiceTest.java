package com.rest.restaurantsystem.backup;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class BackupServiceTest {

    @Mock
    private PostgresToolRunner tools;

    @Test
    void createsAnArchiveWithTheDumpAndItsChecksum() throws Exception {
        byte[] dump = "example PostgreSQL custom archive".getBytes();
        doAnswer(invocation -> {
            List<String> args = invocation.getArgument(1);
            Path path = Path.of(args.stream().filter(arg -> arg.startsWith("--file="))
                    .findFirst().orElseThrow().substring("--file=".length()));
            Files.write(path, dump);
            return null;
        }).when(tools).run(eq("pg_dump"), any(), eq("secret"), any());

        BackupService service = service("jdbc:postgresql://localhost:5432/restaurant_system");
        try (BackupService.BackupArchive archive = service.create()) {
            assertThat(archive.filename()).startsWith("Essential-").endsWith(".zip");
            Map<String, byte[]> entries = readZip(archive.path());
            assertThat(entries).containsOnlyKeys("manifest.json", "database.dump");
            assertThat(entries.get("database.dump")).isEqualTo(dump);
            String manifest = new String(entries.get("manifest.json"));
            String sha256 = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(dump));
            assertThat(manifest).contains("\"database\": \"restaurant_system\"")
                    .contains("\"sha256\": \"" + sha256 + "\"");
            verify(tools).run(eq("pg_restore"), any(), eq("secret"), any());
        }
    }

    @Test
    void rejectsConnectionStringsContainingCredentialsOrParameters() {
        assertThrows(IllegalStateException.class,
                () -> service("jdbc:postgresql://postgres:secret@localhost:5432/restaurant_system").create());
        assertThrows(IllegalStateException.class,
                () -> service("jdbc:postgresql://localhost:5432/restaurant_system?password=secret").create());
    }

    @Test
    void rejectsAnEmptyDump() {
        assertThrows(IOException.class, () -> service("jdbc:postgresql://localhost:5432/restaurant_system").create());
    }

    private BackupService service(String url) {
        return new BackupService(tools, url, "postgres", "secret", "pg_dump", "pg_restore");
    }

    private Map<String, byte[]> readZip(Path path) throws IOException {
        Map<String, byte[]> entries = new HashMap<>();
        try (ZipInputStream input = new ZipInputStream(Files.newInputStream(path))) {
            ZipEntry entry;
            while ((entry = input.getNextEntry()) != null) {
                ByteArrayOutputStream output = new ByteArrayOutputStream();
                input.transferTo(output);
                entries.put(entry.getName(), output.toByteArray());
            }
        }
        return entries;
    }
}
