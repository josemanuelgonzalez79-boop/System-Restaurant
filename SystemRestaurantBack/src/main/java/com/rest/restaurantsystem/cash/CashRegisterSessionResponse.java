package com.rest.restaurantsystem.cash;

import java.math.BigDecimal;
import java.time.Instant;

public record CashRegisterSessionResponse(
        Long id,
        String folio,
        Long branchId,
        String branchName,
        BigDecimal openingAmount,
        String openingNotes,
        CashRegisterStatus status,
        Long openedByUserId,
        String openedByUserName,
        Instant openedAt,
        Long closedByUserId,
        String closedByUserName,
        Instant closedAt,
        BigDecimal expectedCashAtClose,
        BigDecimal countedCashAtClose,
        BigDecimal differenceAtClose,
        String closingNotes,
        long version,
        Instant updatedAt
) {
}
