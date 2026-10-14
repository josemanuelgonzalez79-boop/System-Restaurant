package com.rest.restaurantsystem.inventory;

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
@Table(name = "inventory_movements")
class InventoryMovement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "operation_id", nullable = false, unique = true, updatable = false)
    private UUID operationId;

    @Column(name = "ingredient_id", nullable = false, updatable = false)
    private Long ingredientId;

    @Enumerated(EnumType.STRING)
    @Column(name = "movement_type", nullable = false, length = 20, updatable = false)
    private MovementType movementType;

    @Column(name = "quantity_delta", nullable = false, precision = 14, scale = 3, updatable = false)
    private BigDecimal quantityDelta;

    @Column(name = "stock_after", nullable = false, precision = 14, scale = 3, updatable = false)
    private BigDecimal stockAfter;

    @Column(nullable = false, length = 250, updatable = false)
    private String reason;

    @Column(name = "recorded_by_user_id", nullable = false, updatable = false)
    private Long recordedByUserId;

    @Column(name = "recorded_by_username", nullable = false, length = 80, updatable = false)
    private String recordedByUsername;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt;

    protected InventoryMovement() {
    }

    InventoryMovement(
            UUID operationId, Long ingredientId, MovementType movementType,
            BigDecimal quantityDelta, BigDecimal stockAfter, String reason,
            Long recordedByUserId, String recordedByUsername
    ) {
        this.operationId = operationId;
        this.ingredientId = ingredientId;
        this.movementType = movementType;
        this.quantityDelta = quantityDelta;
        this.stockAfter = stockAfter;
        this.reason = reason.trim();
        this.recordedByUserId = recordedByUserId;
        this.recordedByUsername = recordedByUsername;
    }

    Long getId() {
        return id;
    }

    UUID getOperationId() {
        return operationId;
    }

    Long getIngredientId() {
        return ingredientId;
    }

    MovementType getMovementType() {
        return movementType;
    }

    BigDecimal getQuantityDelta() {
        return quantityDelta;
    }

    BigDecimal getStockAfter() {
        return stockAfter;
    }

    String getReason() {
        return reason;
    }

    Long getRecordedByUserId() {
        return recordedByUserId;
    }

    String getRecordedByUsername() {
        return recordedByUsername;
    }

    Instant getOccurredAt() {
        return occurredAt;
    }

    @PrePersist
    void onCreate() {
        occurredAt = Instant.now();
    }
}
