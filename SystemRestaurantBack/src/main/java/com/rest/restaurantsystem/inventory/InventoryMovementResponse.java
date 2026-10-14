package com.rest.restaurantsystem.inventory;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record InventoryMovementResponse(
        Long id,
        UUID operationId,
        Long ingredientId,
        Long preparationItemId,
        Long reversesMovementId,
        MovementType movementType,
        BigDecimal quantityDelta,
        BigDecimal stockAfter,
        BigDecimal unitCostSnapshot,
        BigDecimal costAmountSnapshot,
        String reason,
        String recordedByUsername,
        Instant occurredAt
) {
    static InventoryMovementResponse from(InventoryMovement movement) {
        return new InventoryMovementResponse(
                movement.getId(), movement.getOperationId(), movement.getIngredientId(),
                movement.getPreparationItemId(), movement.getReversesMovementId(),
                movement.getMovementType(), movement.getQuantityDelta(), movement.getStockAfter(),
                movement.getUnitCostSnapshot(), movement.getCostAmountSnapshot(),
                movement.getReason(), movement.getRecordedByUsername(), movement.getOccurredAt()
        );
    }
}
