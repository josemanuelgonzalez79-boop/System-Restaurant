package com.rest.restaurantsystem.structure;

import java.time.Instant;

public record OperationalAreaResponse(
        Long id,
        Long branchId,
        String branchName,
        String name,
        String description,
        AreaType areaType,
        int sortOrder,
        boolean active,
        long version,
        Instant createdAt,
        Instant updatedAt
) {

    static OperationalAreaResponse from(OperationalArea area) {
        return new OperationalAreaResponse(
                area.getId(),
                area.getBranch().getId(),
                area.getBranch().getName(),
                area.getName(),
                area.getDescription(),
                area.getAreaType(),
                area.getSortOrder(),
                area.isActive(),
                area.getVersion(),
                area.getCreatedAt(),
                area.getUpdatedAt()
        );
    }
}
