package com.rest.restaurantsystem.cash;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "cash_register_movements")
class CashRegisterMovement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "operation_id", nullable = false, unique = true, updatable = false)
    private UUID operationId;

    @Column(name = "session_id", nullable = false, updatable = false)
    private Long sessionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "movement_type", nullable = false, length = 20, updatable = false)
    private CashMovementType movementType;

    @Column(nullable = false, precision = 12, scale = 2, updatable = false)
    private BigDecimal amount;

    @Column(nullable = false, length = 160, updatable = false)
    private String concept;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CashMovementStatus status;

    @Column(name = "recorded_by_user_id", nullable = false, updatable = false)
    private Long recordedByUserId;

    @Column(name = "recorded_at", nullable = false, updatable = false)
    private Instant recordedAt;

    @Column(name = "voided_by_user_id")
    private Long voidedByUserId;

    @Column(name = "voided_at")
    private Instant voidedAt;

    @Column(name = "void_reason", length = 250)
    private String voidReason;

    protected CashRegisterMovement() {
    }

    CashRegisterMovement(
            UUID operationId,
            Long sessionId,
            CashMovementType movementType,
            BigDecimal amount,
            String concept,
            Long recordedByUserId
    ) {
        this.operationId = operationId;
        this.sessionId = sessionId;
        this.movementType = movementType;
        this.amount = amount;
        this.concept = concept.trim();
        this.recordedByUserId = recordedByUserId;
        status = CashMovementStatus.ACTIVE;
    }

    void voidMovement(Long userId, String reason) {
        status = CashMovementStatus.VOIDED;
        voidedByUserId = userId;
        voidedAt = Instant.now();
        voidReason = reason.trim();
    }

    Long getId() {
        return id;
    }

    UUID getOperationId() {
        return operationId;
    }

    Long getSessionId() {
        return sessionId;
    }

    CashMovementType getMovementType() {
        return movementType;
    }

    BigDecimal getAmount() {
        return amount;
    }

    String getConcept() {
        return concept;
    }

    CashMovementStatus getStatus() {
        return status;
    }

    Long getRecordedByUserId() {
        return recordedByUserId;
    }

    Instant getRecordedAt() {
        return recordedAt;
    }

    Long getVoidedByUserId() {
        return voidedByUserId;
    }

    Instant getVoidedAt() {
        return voidedAt;
    }

    String getVoidReason() {
        return voidReason;
    }

    @PrePersist
    void onCreate() {
        recordedAt = Instant.now();
    }
}
