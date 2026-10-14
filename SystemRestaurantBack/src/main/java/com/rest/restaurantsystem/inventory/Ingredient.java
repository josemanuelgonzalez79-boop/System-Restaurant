package com.rest.restaurantsystem.inventory;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "inventory_ingredients")
class Ingredient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private IngredientUnit unit;

    @Column(name = "stock_quantity", nullable = false, precision = 14, scale = 3)
    private BigDecimal stockQuantity;

    @Column(name = "minimum_quantity", nullable = false, precision = 14, scale = 3)
    private BigDecimal minimumQuantity;

    @Column(nullable = false)
    private boolean active;

    @Version
    private long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Ingredient() {
    }

    Ingredient(IngredientRequest request) {
        stockQuantity = BigDecimal.ZERO.setScale(3);
        active = true;
        update(request);
    }

    void update(IngredientRequest request) {
        name = request.name().trim();
        unit = request.unit();
        minimumQuantity = request.minimumQuantity().setScale(3);
    }

    void setActive(boolean active) {
        this.active = active;
    }

    void setStockQuantity(BigDecimal stockQuantity) {
        this.stockQuantity = stockQuantity;
    }

    Long getId() {
        return id;
    }

    String getName() {
        return name;
    }

    IngredientUnit getUnit() {
        return unit;
    }

    BigDecimal getStockQuantity() {
        return stockQuantity;
    }

    BigDecimal getMinimumQuantity() {
        return minimumQuantity;
    }

    boolean isActive() {
        return active;
    }

    long getVersion() {
        return version;
    }

    Instant getUpdatedAt() {
        return updatedAt;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
