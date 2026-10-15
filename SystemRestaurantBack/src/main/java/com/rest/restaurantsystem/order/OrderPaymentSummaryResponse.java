package com.rest.restaurantsystem.order;

import java.math.BigDecimal;
import java.util.List;

public record OrderPaymentSummaryResponse(
        OrderDetailResponse orderDetail,
        List<PaymentResponse> payments,
        List<OrderDiscountResponse> discounts,
        BigDecimal subtotal,
        BigDecimal discountAmount,
        BigDecimal total,
        BigDecimal paid,
        BigDecimal tipTotal,
        BigDecimal balance,
        boolean settled,
        boolean canClose,
        String closeBlockingReason,
        Long openCashRegisterSessionId,
        String openCashRegisterSessionFolio
) {
}
