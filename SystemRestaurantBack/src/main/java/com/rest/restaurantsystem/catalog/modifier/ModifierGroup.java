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

import java.time.Instant;

@Entity
@Table(name = "product_modifier_groups")
class ModifierGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "min_selections", nullable = false)
    private int minSelections;

    @Column(name = "max_selections", nullable = false)
    private int maxSelections;

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

    protected ModifierGroup() {
    }

    ModifierGroup(ModifierGroupRequest request) {
        active = true;
        apply(request);
    }

    void update(ModifierGroupRequest request) {
        apply(request);
    }

    void setActive(boolean active) {
        this.active = active;
    }

    Long getId() {
        return id;
    }

    Long getProductId() {
        return productId;
    }

    String getName() {
        return name;
    }

    int getMinSelections() {
        return minSelections;
    }

    int getMaxSelections() {
        return maxSelections;
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

    private void apply(ModifierGroupRequest request) {
        productId = request.productId();
        name = request.name().trim();
        minSelections = request.minSelections();
        maxSelections = request.maxSelections();
        sortOrder = request.sortOrder();
    }
}
