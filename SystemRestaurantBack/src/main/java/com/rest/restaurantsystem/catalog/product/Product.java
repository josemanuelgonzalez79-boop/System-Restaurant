package com.rest.restaurantsystem.catalog.product;

import com.rest.restaurantsystem.catalog.category.Category;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "products")
class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(length = 40)
    private String sku;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProductDestination destination;

    @Column(nullable = false)
    private boolean active;

    @Column(nullable = false)
    private boolean available;

    @Version
    private long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Product() {
    }

    Product(ProductRequest request, Category category) {
        active = true;
        available = true;
        apply(request, category);
    }

    void update(ProductRequest request, Category category) {
        apply(request, category);
    }

    void setActive(boolean active) {
        this.active = active;
    }

    void setAvailable(boolean available) {
        this.available = available;
    }

    Long getId() {
        return id;
    }

    Category getCategory() {
        return category;
    }

    String getSku() {
        return sku;
    }

    String getName() {
        return name;
    }

    String getDescription() {
        return description;
    }

    BigDecimal getPrice() {
        return price;
    }

    ProductDestination getDestination() {
        return destination;
    }

    boolean isActive() {
        return active;
    }

    boolean isAvailable() {
        return available;
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

    private void apply(ProductRequest request, Category category) {
        this.category = category;
        sku = normalizeOptional(request.sku());
        name = request.name().trim();
        description = normalizeOptional(request.description());
        price = request.price();
        destination = request.destination();
    }

    private String normalizeOptional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
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
