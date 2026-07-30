package com.rest.restaurantsystem.catalog.category;

import java.time.Instant;

public record CategoryResponse(
        Long id,
        String name,
        String description,
        int sortOrder,
        boolean active,
        long version,
        Instant createdAt,
        Instant updatedAt
) {

    static CategoryResponse from(Category category) {
        return new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getDescription(),
                category.getSortOrder(),
                category.isActive(),
                category.getVersion(),
                category.getCreatedAt(),
                category.getUpdatedAt()
        );
    }
}
