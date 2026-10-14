package com.rest.restaurantsystem.inventory;

import java.math.BigDecimal;

public record RecipeLineResponse(
        Long ingredientId,
        String ingredientName,
        IngredientUnit unit,
        boolean ingredientActive,
        BigDecimal quantity,
        BigDecimal stockQuantity
) {
    static RecipeLineResponse from(RecipeLine line, Ingredient ingredient) {
        return new RecipeLineResponse(
                line.getIngredientId(), ingredient.getName(), ingredient.getUnit(),
                ingredient.isActive(), line.getQuantity(), ingredient.getStockQuantity()
        );
    }
}
