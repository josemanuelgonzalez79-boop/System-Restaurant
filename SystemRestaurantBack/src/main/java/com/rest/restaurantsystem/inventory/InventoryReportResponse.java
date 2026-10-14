package com.rest.restaurantsystem.inventory;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record InventoryReportResponse(
        LocalDate from, LocalDate to, String timezone, Instant generatedAt,
        int activeIngredients, int lowStock, int outOfStock, int missingCosts,
        int unvaluedStock, BigDecimal knownStockValue, BigDecimal knownOrderCost,
        long unpricedOrderMovements, List<IngredientSummary> ingredients
) {
    public record IngredientSummary(
            Long id, String name, IngredientUnit unit, boolean active,
            BigDecimal stockQuantity, BigDecimal minimumQuantity, BigDecimal unitCost,
            BigDecimal stockValue, boolean lowStock, boolean outOfStock,
            BigDecimal purchases, BigDecimal manualUse, BigDecimal waste,
            BigDecimal adjustments, BigDecimal orderConsumption, BigDecimal orderReversal,
            BigDecimal netDelta, BigDecimal knownOrderCost, long unpricedOrderMovements
    ) { }
}
