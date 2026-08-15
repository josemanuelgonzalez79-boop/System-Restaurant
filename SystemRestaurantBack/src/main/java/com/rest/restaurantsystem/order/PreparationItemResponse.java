package com.rest.restaurantsystem.order;

import java.time.Instant;
import java.util.List;

public record PreparationItemResponse(
        Long id,
        Long orderItemId,
        String productName,
        int quantity,
        String notes,
        PreparationStatus status,
        List<PreparationModifierResponse> modifiers,
        long version,
        Instant startedAt,
        Instant readyAt,
        Instant deliveredAt,
        Instant cancelledAt,
        Instant createdAt,
        Instant updatedAt
) {
}
