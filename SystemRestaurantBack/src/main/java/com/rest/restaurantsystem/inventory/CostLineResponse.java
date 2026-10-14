package com.rest.restaurantsystem.inventory;

import java.math.BigDecimal;

public record CostLineResponse(
        Long ingredientId, String ingredientName, IngredientUnit unit,
        BigDecimal quantity, BigDecimal unitCost, BigDecimal lineCost
) {
}
