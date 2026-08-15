package com.rest.restaurantsystem.order;

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
@Table(name = "order_payments")
class OrderPayment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "operation_id", nullable = false, updatable = false, unique = true)
    private UUID operationId;

    @Column(name = "order_id", nullable = false, updatable = false)
    private Long orderId;

    @Column(nullable = false, precision = 12, scale = 2, updatable = false)
    private BigDecimal amount;

    @Column(name = "tendered_amount", nullable = false, precision = 12, scale = 2, updatable = false)
    private BigDecimal tenderedAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20, updatable = false)
    private PaymentMethod method;

    @Column(name = "method_label", length = 60, updatable = false)
    private String methodLabel;

    @Column(length = 120, updatable = false)
    private String reference;

    @Column(length = 250, updatable = false)
    private String notes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus status;

    @Column(name = "received_by_user_id", nullable = false, updatable = false)
    private Long receivedByUserId;

    @Column(name = "received_at", nullable = false, updatable = false)
    private Instant receivedAt;

    @Column(name = "voided_by_user_id")
    private Long voidedByUserId;

    @Column(name = "voided_at")
    private Instant voidedAt;

    @Column(name = "void_reason", length = 250)
    private String voidReason;

    protected OrderPayment() {
    }

    OrderPayment(
            UUID operationId,
            Long orderId,
            BigDecimal amount,
            BigDecimal tenderedAmount,
            PaymentMethod method,
            String methodLabel,
            String reference,
            String notes,
            Long receivedByUserId
    ) {
        this.operationId = operationId;
        this.orderId = orderId;
        this.amount = amount;
        this.tenderedAmount = tenderedAmount;
        this.method = method;
        this.methodLabel = normalizeOptional(methodLabel);
        this.reference = normalizeOptional(reference);
        this.notes = normalizeOptional(notes);
        this.receivedByUserId = receivedByUserId;
        status = PaymentStatus.ACTIVE;
    }

    void voidPayment(Long userId, String reason) {
        status = PaymentStatus.VOIDED;
        voidedByUserId = userId;
        voidedAt = Instant.now();
        voidReason = reason.trim();
    }

    BigDecimal getChangeAmount() {
        return tenderedAmount.subtract(amount);
    }

    Long getId() {
        return id;
    }

    UUID getOperationId() {
        return operationId;
    }

    Long getOrderId() {
        return orderId;
    }

    BigDecimal getAmount() {
        return amount;
    }

    BigDecimal getTenderedAmount() {
        return tenderedAmount;
    }

    PaymentMethod getMethod() {
        return method;
    }

    String getMethodLabel() {
        return methodLabel;
    }

    String getReference() {
        return reference;
    }

    String getNotes() {
        return notes;
    }

    PaymentStatus getStatus() {
        return status;
    }

    Long getReceivedByUserId() {
        return receivedByUserId;
    }

    Instant getReceivedAt() {
        return receivedAt;
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
        receivedAt = Instant.now();
    }

    private String normalizeOptional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
