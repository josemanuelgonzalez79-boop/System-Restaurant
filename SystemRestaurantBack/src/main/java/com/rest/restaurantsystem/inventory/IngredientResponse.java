package com.rest.restaurantsystem.inventory;

import java.math.BigDecimal;
import java.time.Instant;

public record IngredientResponse(
        Long id,
        String name,
        IngredientUnit unit,
        BigDecimal stockQuantity,
        BigDecimal minimumQuantity,
        boolean lowStock,
        boolean active,
        long version,
        Instant updatedAt
) {
    static IngredientResponse from(Ingredient ingredient) {
        return new IngredientResponse(
                ingredient.getId(), ingredient.getName(), ingredient.getUnit(),
                ingredient.getStockQuantity(), ingredient.getMinimumQuantity(),
                ingredient.isActive() && ingredient.getMinimumQuantity().signum() > 0
                        && ingredient.getStockQuantity().compareTo(ingredient.getMinimumQuantity()) <= 0,
                ingredient.isActive(), ingredient.getVersion(), ingredient.getUpdatedAt()
        );
    }
}
