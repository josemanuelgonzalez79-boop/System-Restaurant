package com.rest.restaurantsystem.catalog.modifier;

import java.math.BigDecimal;
import java.time.Instant;

public record ModifierOptionResponse(
        Long id,
        Long groupId,
        String groupName,
        Long productId,
        String name,
        BigDecimal priceDelta,
        int sortOrder,
        boolean active,
        long version,
        Instant createdAt,
        Instant updatedAt
) {

    static ModifierOptionResponse from(ModifierOption option, ModifierGroup group) {
        return new ModifierOptionResponse(
                option.getId(),
                option.getGroupId(),
                group.getName(),
                group.getProductId(),
                option.getName(),
                option.getPriceDelta(),
                option.getSortOrder(),
                option.isActive(),
                option.getVersion(),
                option.getCreatedAt(),
                option.getUpdatedAt()
        );
    }
}
