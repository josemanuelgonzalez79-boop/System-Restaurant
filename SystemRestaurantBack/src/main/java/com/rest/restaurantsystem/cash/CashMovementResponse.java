package com.rest.restaurantsystem.cash;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CashMovementResponse(
        Long id,
        String folio,
        UUID operationId,
        CashMovementType movementType,
        BigDecimal amount,
        String concept,
        CashMovementStatus status,
        Long recordedByUserId,
        String recordedByUserName,
        Instant recordedAt,
        Long voidedByUserId,
        String voidedByUserName,
        Instant voidedAt,
        String voidReason
) {
}
