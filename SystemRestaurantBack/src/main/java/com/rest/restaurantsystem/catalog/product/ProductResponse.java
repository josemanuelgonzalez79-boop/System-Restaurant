package com.rest.restaurantsystem.catalog.product;

import java.math.BigDecimal;
import java.time.Instant;

public record ProductResponse(
        Long id,
        Long categoryId,
        String categoryName,
        String sku,
        String name,
        String description,
        BigDecimal price,
        ProductDestination destination,
        boolean active,
        boolean available,
        long version,
        Instant createdAt,
        Instant updatedAt
) {

    static ProductResponse from(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getCategory().getId(),
                product.getCategory().getName(),
                product.getSku(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getDestination(),
                product.isActive(),
                product.isAvailable(),
                product.getVersion(),
                product.getCreatedAt(),
                product.getUpdatedAt()
        );
    }
}
