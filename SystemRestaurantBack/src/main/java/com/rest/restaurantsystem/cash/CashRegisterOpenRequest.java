package com.rest.restaurantsystem.cash;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CashRegisterOpenRequest(
        @NotNull Long branchId,
        @NotNull @DecimalMin("0.00") @Digits(integer = 10, fraction = 2) BigDecimal openingAmount,
        @Size(max = 250) String notes
) {
}
