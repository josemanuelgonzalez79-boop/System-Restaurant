package com.rest.restaurantsystem.cash;

import java.math.BigDecimal;
import java.util.List;

public record CashRegisterSummaryResponse(
        CashRegisterSessionResponse session,
        List<CashMovementResponse> movements,
        BigDecimal cashPayments,
        BigDecimal cardPayments,
        BigDecimal transferPayments,
        BigDecimal otherPayments,
        BigDecimal cashIn,
        BigDecimal cashOut,
        BigDecimal expectedCash,
        boolean canClose,
        String closeBlockingReason
) {
}
