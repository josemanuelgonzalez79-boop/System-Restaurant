package com.rest.restaurantsystem.inventory;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;

@Entity
@Table(name = "inventory_recipes")
class Recipe {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_id")
    private Long productId;

    @Column(name = "modifier_option_id")
    private Long modifierOptionId;

    @Version
    private long version;

    @Column(name = "updated_by_user_id", nullable = false)
    private Long updatedByUserId;

    @Column(name = "updated_by_username", nullable = false, length = 80)
    private String updatedByUsername;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Recipe() {
    }

    Recipe(RecipeOwnerType ownerType, Long ownerId, Long userId, String username) {
        productId = ownerType == RecipeOwnerType.PRODUCT ? ownerId : null;
        modifierOptionId = ownerType == RecipeOwnerType.MODIFIER_OPTION ? ownerId : null;
        updateEditor(userId, username);
    }

    void updateEditor(Long userId, String username) {
        updatedByUserId = userId;
        updatedByUsername = username;
        updatedAt = Instant.now();
    }

    Long getId() {
        return id;
    }

    Long getProductId() {
        return productId;
    }

    Long getModifierOptionId() {
        return modifierOptionId;
    }

    long getVersion() {
        return version;
    }

    String getUpdatedByUsername() {
        return updatedByUsername;
    }

    Instant getUpdatedAt() {
        return updatedAt;
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
        updatedAt = createdAt;
    }
}
