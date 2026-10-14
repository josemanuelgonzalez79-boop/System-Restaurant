package com.rest.restaurantsystem.inventory;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CostUpdateRequest(
        @NotNull @DecimalMin("0.0000") @Digits(integer = 10, fraction = 4) BigDecimal unitCost,
        @NotNull @Min(0) Long expectedVersion,
        @NotBlank @Size(min = 5, max = 250) String reason
) {
}
