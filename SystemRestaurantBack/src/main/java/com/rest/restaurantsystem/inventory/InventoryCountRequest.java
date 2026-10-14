package com.rest.restaurantsystem.inventory;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record InventoryCountRequest(
        @NotNull UUID operationId,
        @NotBlank @Size(min = 5, max = 250) String reason,
        @NotEmpty List<@Valid Line> lines
) {
    public record Line(
            @NotNull Long ingredientId,
            @NotNull @Min(0) Long expectedVersion,
            @NotNull @DecimalMin("0.000") @Digits(integer = 11, fraction = 3) BigDecimal countedQuantity
    ) { }
}
