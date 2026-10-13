package com.rest.restaurantsystem.backup;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PostgresToolRunnerTest {

    @Test
    void explainsHowToConfigureTheToolsWhenTheExecutableCannotStart() {
        String missing = Path.of(System.getProperty("java.io.tmpdir"),
                "essential-missing-pg-dump-" + UUID.randomUUID()).toString();

        IOException error = assertThrows(IOException.class,
                () -> new PostgresToolRunner().run(missing, List.of(), "unused", Duration.ofSeconds(1)));

        assertThat(error.getMessage()).contains("No se pudo iniciar").contains("APP_BACKUP_PG_DUMP");
    }
}
