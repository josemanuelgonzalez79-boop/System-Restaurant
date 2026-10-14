package com.rest.restaurantsystem.inventory;

import java.math.BigDecimal;
import java.time.Instant;

public record CostChangeResponse(
        Long id, Long ingredientId, BigDecimal previousUnitCost, BigDecimal newUnitCost,
        String reason, String changedByUsername, Instant changedAt
) {
    static CostChangeResponse from(CostChange change) {
        return new CostChangeResponse(change.getId(), change.getIngredientId(),
                change.getPreviousUnitCost(), change.getNewUnitCost(), change.getReason(),
                change.getChangedByUsername(), change.getChangedAt());
    }
}
