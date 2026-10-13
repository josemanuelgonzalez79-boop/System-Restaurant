package com.rest.restaurantsystem.backup;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

@Component
public class PostgresToolRunner {

    public void run(String executable, List<String> arguments, String password, Duration timeout)
            throws IOException, InterruptedException {
        String tool = Path.of(executable).getFileName().toString();
        String resolved = resolve(executable);
        List<String> command = new ArrayList<>();
        command.add(resolved);
        command.addAll(arguments);
        Path errorFile = Files.createTempFile("essential-postgres-tool-", ".log");
        try {
            runProcess(command, password, timeout, tool, errorFile);
        } finally {
            Files.deleteIfExists(errorFile);
        }
    }

    private void runProcess(List<String> command, String password, Duration timeout,
                            String tool, Path errorFile) throws IOException, InterruptedException {
        ProcessBuilder builder = new ProcessBuilder(command)
                .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                .redirectError(errorFile.toFile());
        builder.environment().put("PGPASSWORD", password);
        builder.environment().put("PGCONNECT_TIMEOUT", "10");
        Process process;
        try {
            process = builder.start();
        } catch (IOException exception) {
            throw new IOException("No se pudo iniciar " + tool + ". Revisa que las herramientas "
                    + "PostgreSQL estén instaladas o configura APP_BACKUP_PG_DUMP y "
                    + "APP_BACKUP_PG_RESTORE con rutas completas.", exception);
        }
        try {
            if (!process.waitFor(timeout.toSeconds(), TimeUnit.SECONDS)) {
                process.destroyForcibly();
                process.waitFor();
                throw new IOException(tool + " excedió el tiempo permitido.");
            }
            if (process.exitValue() != 0) {
                throw new IOException(describeFailure(tool, process.exitValue(), errorFile));
            }
        } catch (InterruptedException exception) {
            process.destroyForcibly();
            Thread.currentThread().interrupt();
            throw exception;
        }
    }

    private String resolve(String executable) throws IOException {
        if (executable.contains("/") || executable.contains("\\")
                || !System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win")) {
            return executable;
        }
        for (String variable : List.of("ProgramW6432", "ProgramFiles", "ProgramFiles(x86)")) {
            String location = System.getenv(variable);
            if (location == null || location.isBlank()) continue;
            Path installations = Path.of(location, "PostgreSQL");
            if (!Files.isDirectory(installations)) continue;
            try (var versions = Files.list(installations)) {
                String filename = executable.endsWith(".exe") ? executable : executable + ".exe";
                Path match = versions.filter(Files::isDirectory)
                        .sorted(Comparator.comparingInt((Path path) -> version(path.getFileName().toString()))
                                .reversed())
                        .map(path -> path.resolve("bin").resolve(filename))
                        .filter(Files::isRegularFile)
                        .findFirst().orElse(null);
                if (match != null) return match.toString();
            }
        }
        return executable;
    }

    private int version(String name) {
        try {
            return Integer.parseInt(name);
        } catch (NumberFormatException ignored) {
            return -1;
        }
    }

    private String describeFailure(String tool, int exitCode, Path errorFile) throws IOException {
        String output;
        try (InputStream input = Files.newInputStream(errorFile)) {
            output = new String(input.readNBytes(8192), StandardCharsets.UTF_8)
                    .toLowerCase(Locale.ROOT);
        }
        if (output.contains("server version mismatch") || output.contains("unsupported version")) {
            return tool + " no es compatible con la versión del servidor PostgreSQL. "
                    + "Instala las herramientas de la misma versión o una más reciente.";
        }
        if (output.contains("password authentication failed") || output.contains("autenticación")
                || output.contains("autenticacion")) {
            return tool + " no pudo autenticarse en PostgreSQL. Revisa DB_USERNAME y DB_PASSWORD "
                    + "en la configuración del backend.";
        }
        if (output.contains("connection refused") || output.contains("conexión rechazada")
                || output.contains("no se pudo conectar")) {
            return tool + " no pudo conectar con PostgreSQL. Revisa DB_URL y el servicio de la base.";
        }
        if (output.contains("permission denied") || output.contains("permiso denegado")) {
            return tool + " recibió un error de permisos. Revisa el usuario de PostgreSQL "
                    + "y el acceso a la carpeta temporal del servidor.";
        }
        return tool + " terminó con código " + exitCode + ". Revisa la versión de las herramientas "
                + "y la conexión PostgreSQL del backend.";
    }
}
