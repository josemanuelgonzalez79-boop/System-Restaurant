package com.rest.restaurantsystem.health;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class HealthService {

    private final JdbcTemplate jdbcTemplate;
    private final String applicationName;

    public HealthService(
            JdbcTemplate jdbcTemplate,
            @Value("${spring.application.name}") String applicationName
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.applicationName = applicationName;
    }

    public HealthResponse getHealth() {
        boolean databaseAvailable = databaseIsAvailable();

        return new HealthResponse(
                applicationName,
                databaseAvailable ? "UP" : "DEGRADED",
                databaseAvailable ? "UP" : "DOWN",
                Instant.now()
        );
    }

    private boolean databaseIsAvailable() {
        try {
            Integer result = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            return result != null && result == 1;
        } catch (RuntimeException exception) {
            return false;
        }
    }
}
