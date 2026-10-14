package com.rest.restaurantsystem.inventory;

import java.time.Instant;
import java.util.List;

public record RecipeResponse(
        Long id,
        RecipeOwnerType ownerType,
        Long ownerId,
        String ownerName,
        Long version,
        String updatedByUsername,
        Instant updatedAt,
        List<RecipeLineResponse> lines
) {
}
