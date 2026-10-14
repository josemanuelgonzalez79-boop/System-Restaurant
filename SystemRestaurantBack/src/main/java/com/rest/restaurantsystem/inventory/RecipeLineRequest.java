package com.rest.restaurantsystem.inventory;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record RecipeLineRequest(
        @NotNull Long ingredientId,
        @NotNull @DecimalMin("0.001") @Digits(integer = 11, fraction = 3) BigDecimal quantity
) {
}
