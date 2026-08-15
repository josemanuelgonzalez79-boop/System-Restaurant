package com.rest.restaurantsystem.catalog.modifier;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "product_modifier_options")
class ModifierOption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "group_id", nullable = false)
    private Long groupId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "price_delta", nullable = false, precision = 12, scale = 2)
    private BigDecimal priceDelta;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(nullable = false)
    private boolean active;

    @Version
    private long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected ModifierOption() {
    }

    ModifierOption(ModifierOptionRequest request) {
        active = true;
        apply(request);
    }

    void update(ModifierOptionRequest request) {
        apply(request);
    }

    void setActive(boolean active) {
        this.active = active;
    }

    Long getId() {
        return id;
    }

    Long getGroupId() {
        return groupId;
    }

    String getName() {
        return name;
    }

    BigDecimal getPriceDelta() {
        return priceDelta;
    }

    int getSortOrder() {
        return sortOrder;
    }

    boolean isActive() {
        return active;
    }

    long getVersion() {
        return version;
    }

    Instant getCreatedAt() {
        return createdAt;
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

    private void apply(ModifierOptionRequest request) {
        groupId = request.groupId();
        name = request.name().trim();
        priceDelta = request.priceDelta();
        sortOrder = request.sortOrder();
    }
}
