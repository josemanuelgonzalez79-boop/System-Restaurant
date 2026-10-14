package com.rest.restaurantsystem.inventory;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "inventory_cost_changes")
class CostChange {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ingredient_id", nullable = false, updatable = false)
    private Long ingredientId;

    @Column(name = "previous_unit_cost", precision = 14, scale = 4, updatable = false)
    private BigDecimal previousUnitCost;

    @Column(name = "new_unit_cost", nullable = false, precision = 14, scale = 4, updatable = false)
    private BigDecimal newUnitCost;

    @Column(nullable = false, length = 250, updatable = false)
    private String reason;

    @Column(name = "changed_by_user_id", nullable = false, updatable = false)
    private Long changedByUserId;

    @Column(name = "changed_by_username", nullable = false, length = 80, updatable = false)
    private String changedByUsername;

    @Column(name = "changed_at", nullable = false, updatable = false)
    private Instant changedAt;

    protected CostChange() {
    }

    CostChange(Long ingredientId, BigDecimal previousUnitCost, BigDecimal newUnitCost,
               String reason, Long changedByUserId, String changedByUsername) {
        this.ingredientId = ingredientId;
        this.previousUnitCost = previousUnitCost;
        this.newUnitCost = newUnitCost;
        this.reason = reason.trim();
        this.changedByUserId = changedByUserId;
        this.changedByUsername = changedByUsername;
    }

    Long getId() { return id; }
    Long getIngredientId() { return ingredientId; }
    BigDecimal getPreviousUnitCost() { return previousUnitCost; }
    BigDecimal getNewUnitCost() { return newUnitCost; }
    String getReason() { return reason; }
    String getChangedByUsername() { return changedByUsername; }
    Instant getChangedAt() { return changedAt; }

    @PrePersist
    void onCreate() { changedAt = Instant.now(); }
}
