package com.rest.restaurantsystem.catalog.modifier;

import java.time.Instant;
import java.util.List;

public record ModifierGroupResponse(
        Long id,
        Long productId,
        String productName,
        String name,
        int minSelections,
        int maxSelections,
        int sortOrder,
        boolean active,
        long version,
        Instant createdAt,
        Instant updatedAt,
        List<ModifierOptionResponse> options
) {
}
