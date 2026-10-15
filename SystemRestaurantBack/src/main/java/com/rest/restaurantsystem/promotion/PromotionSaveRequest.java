package com.rest.restaurantsystem.promotion;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PromotionSaveRequest(
        @NotBlank @Size(max = 24) String code,
        @NotBlank @Size(max = 100) String name,
        @NotNull DiscountType discountType,
        @NotNull @DecimalMin("0.01") @Digits(integer = 10, fraction = 2) BigDecimal discountValue,
        @NotNull @DecimalMin("0.00") @Digits(integer = 10, fraction = 2) BigDecimal minimumSubtotal,
        @NotNull LocalDate startsOn,
        @NotNull LocalDate endsOn,
        @NotNull Boolean active,
        Long expectedVersion
) { }
