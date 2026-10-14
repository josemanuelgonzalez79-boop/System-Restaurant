package com.rest.restaurantsystem.inventory;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

public record StockMovementRequest(
        @NotNull UUID operationId,
        @NotNull MovementType movementType,
        @NotNull @DecimalMin("0.000") @Digits(integer = 11, fraction = 3) BigDecimal quantity,
        @NotBlank @Size(max = 250) String reason,
        @NotNull @Min(0) Long expectedVersion
) {
}
