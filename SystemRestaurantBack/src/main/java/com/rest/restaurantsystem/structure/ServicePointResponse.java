package com.rest.restaurantsystem.structure;

import java.time.Instant;

public record ServicePointResponse(
        Long id,
        Long areaId,
        String areaName,
        Long branchId,
        String branchName,
        String code,
        String name,
        String description,
        ServicePointType pointType,
        int sortOrder,
        boolean active,
        long version,
        Instant createdAt,
        Instant updatedAt
) {

    static ServicePointResponse from(ServicePoint point) {
        OperationalArea area = point.getArea();
        return new ServicePointResponse(
                point.getId(),
                area.getId(),
                area.getName(),
                area.getBranch().getId(),
                area.getBranch().getName(),
                point.getCode(),
                point.getName(),
                point.getDescription(),
                point.getPointType(),
                point.getSortOrder(),
                point.isActive(),
                point.getVersion(),
                point.getCreatedAt(),
                point.getUpdatedAt()
        );
    }
}
