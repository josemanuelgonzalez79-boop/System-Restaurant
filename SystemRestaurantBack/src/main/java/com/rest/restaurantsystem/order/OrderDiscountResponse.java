package com.rest.restaurantsystem.order;

import com.rest.restaurantsystem.promotion.DiscountType;

import java.math.BigDecimal;
import java.time.Instant;

public record OrderDiscountResponse(
        Long id, String source, String code, String name, DiscountType discountType,
        BigDecimal discountValue, BigDecimal baseSubtotal, BigDecimal amount, String reason,
        String status, String appliedByUserName, Instant appliedAt,
        String voidedByUserName, Instant voidedAt, String voidReason
) { }
