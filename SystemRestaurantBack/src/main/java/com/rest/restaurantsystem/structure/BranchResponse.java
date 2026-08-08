package com.rest.restaurantsystem.structure;

import java.time.Instant;

public record BranchResponse(
        Long id,
        String code,
        String name,
        String address,
        String phone,
        String timezone,
        int sortOrder,
        boolean active,
        long version,
        Instant createdAt,
        Instant updatedAt
) {

    static BranchResponse from(Branch branch) {
        return new BranchResponse(
                branch.getId(),
                branch.getCode(),
                branch.getName(),
                branch.getAddress(),
                branch.getPhone(),
                branch.getTimezone(),
                branch.getSortOrder(),
                branch.isActive(),
                branch.getVersion(),
                branch.getCreatedAt(),
                branch.getUpdatedAt()
        );
    }
}
