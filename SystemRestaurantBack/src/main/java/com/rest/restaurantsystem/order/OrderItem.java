package com.rest.restaurantsystem.order;

import com.rest.restaurantsystem.catalog.product.ProductDestination;
import com.rest.restaurantsystem.catalog.product.ProductResponse;
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
@Table(name = "order_items")
class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "product_name", nullable = false, length = 120)
    private String productName;

    @Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProductDestination destination;

    @Column(nullable = false)
    private int quantity;

    @Column(length = 500)
    private String notes;

    @Column(name = "sent_at")
    private Instant sentAt;

    @Version
    private long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected OrderItem() {
    }

    OrderItem(Long orderId, ProductResponse product, int quantity, String notes) {
        this.orderId = orderId;
        productId = product.id();
        productName = product.name();
        unitPrice = product.price();
        destination = product.destination();
        update(quantity, notes);
    }

    void update(int quantity, String notes) {
        this.quantity = quantity;
        this.notes = normalizeOptional(notes);
    }

    void markSent(Instant sentAt) {
        this.sentAt = sentAt;
    }

    Long getId() {
        return id;
    }

    Long getOrderId() {
        return orderId;
    }

    Long getProductId() {
        return productId;
    }

    String getProductName() {
        return productName;
    }

    BigDecimal getUnitPrice() {
        return unitPrice;
    }

    ProductDestination getDestination() {
        return destination;
    }

    int getQuantity() {
        return quantity;
    }

    String getNotes() {
        return notes;
    }

    Instant getSentAt() {
        return sentAt;
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

    private String normalizeOptional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
