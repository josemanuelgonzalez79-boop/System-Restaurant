package com.rest.restaurantsystem.order;

import com.rest.restaurantsystem.catalog.product.ProductDestination;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderItemResponse(
        Long id,
        Long productId,
        String productName,
        BigDecimal unitPrice,
        ProductDestination destination,
        int quantity,
        String notes,
        List<OrderItemModifierResponse> modifiers,
        BigDecimal baseSubtotal,
        BigDecimal modifierSubtotal,
        BigDecimal lineTotal,
        Instant sentAt,
        long version,
        Instant createdAt,
        Instant updatedAt
) {
}
