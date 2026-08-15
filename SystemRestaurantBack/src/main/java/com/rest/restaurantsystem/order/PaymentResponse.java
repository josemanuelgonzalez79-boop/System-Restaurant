package com.rest.restaurantsystem.order;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentResponse(
        Long id,
        String folio,
        UUID operationId,
        BigDecimal amount,
        BigDecimal tenderedAmount,
        BigDecimal changeAmount,
        PaymentMethod method,
        String methodLabel,
        String reference,
        String notes,
        PaymentStatus status,
        Long receivedByUserId,
        String receivedByUserName,
        Instant receivedAt,
        Long voidedByUserId,
        String voidedByUserName,
        Instant voidedAt,
        String voidReason
) {
}
