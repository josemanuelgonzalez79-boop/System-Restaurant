package com.rest.restaurantsystem.inventory;

import java.util.List;

/** A frozen order item ready to be sent to kitchen or service. */
public record ConsumptionRequest(
        Long preparationItemId,
        Long orderId,
        Long productId,
        String productName,
        int quantity,
        List<Long> modifierOptionIds
) {
}
