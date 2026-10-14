package com.rest.restaurantsystem.inventory;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record IngredientRequest(
        @NotBlank @Size(max = 120) String name,
        @NotNull IngredientUnit unit,
        @NotNull @DecimalMin("0.000") @Digits(integer = 11, fraction = 3) BigDecimal minimumQuantity,
        Long version
) {
}
