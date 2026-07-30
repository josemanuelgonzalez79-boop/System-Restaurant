package com.rest.restaurantsystem.health;

import java.time.Instant;

public record HealthResponse(
        String application,
        String status,
        String database,
        Instant timestamp
) {
}
