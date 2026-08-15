package com.rest.restaurantsystem.order;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

public record PaymentCreateRequest(
        @NotNull UUID operationId,
        @NotNull @DecimalMin("0.01") @Digits(integer = 10, fraction = 2) BigDecimal amount,
        @DecimalMin("0.01") @Digits(integer = 10, fraction = 2) BigDecimal tenderedAmount,
        @NotNull PaymentMethod method,
        @Size(max = 60) String methodLabel,
        @Size(max = 120) String reference,
        @Size(max = 250) String notes,
        long orderVersion
) {
}
