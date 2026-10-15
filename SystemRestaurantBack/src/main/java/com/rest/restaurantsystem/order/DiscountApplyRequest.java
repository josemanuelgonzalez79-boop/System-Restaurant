package com.rest.restaurantsystem.order;

import com.rest.restaurantsystem.promotion.DiscountType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

public record DiscountApplyRequest(
        @NotNull UUID operationId,
        Long promotionId,
        DiscountType manualType,
        @DecimalMin("0.01") @Digits(integer = 10, fraction = 2) BigDecimal manualValue,
        @Size(max = 250) String reason,
        long orderVersion
) { }
