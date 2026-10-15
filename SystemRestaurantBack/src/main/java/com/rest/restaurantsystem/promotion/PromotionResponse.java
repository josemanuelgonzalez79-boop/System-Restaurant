package com.rest.restaurantsystem.promotion;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record PromotionResponse(Long id, String code, String name, DiscountType discountType,
                                BigDecimal discountValue, BigDecimal minimumSubtotal,
                                LocalDate startsOn, LocalDate endsOn, boolean active, long version,
                                String updatedByUsername, Instant updatedAt) {
    static PromotionResponse from(Promotion promotion) {
        return new PromotionResponse(promotion.getId(), promotion.getCode(), promotion.getName(),
                promotion.getDiscountType(), promotion.getDiscountValue(),
                promotion.getMinimumSubtotal(), promotion.getStartsOn(), promotion.getEndsOn(),
                promotion.isActive(), promotion.getVersion(), promotion.getUpdatedByUsername(),
                promotion.getUpdatedAt());
    }
}
