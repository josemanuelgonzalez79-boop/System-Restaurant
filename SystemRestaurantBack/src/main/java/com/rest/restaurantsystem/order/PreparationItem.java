package com.rest.restaurantsystem.order;

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

import java.time.Instant;

@Entity
@Table(name = "preparation_items")
class PreparationItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ticket_id", nullable = false)
    private Long ticketId;

    @Column(name = "order_item_id", nullable = false, updatable = false)
    private Long orderItemId;

    @Column(name = "product_name", nullable = false, length = 120, updatable = false)
    private String productName;

    @Column(nullable = false, updatable = false)
    private int quantity;

    @Column(length = 500, updatable = false)
    private String notes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PreparationStatus status;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "ready_at")
    private Instant readyAt;

    @Column(name = "delivered_at")
    private Instant deliveredAt;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Version
    private long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected PreparationItem() {
    }

    PreparationItem(Long ticketId, OrderItem item) {
        this.ticketId = ticketId;
        orderItemId = item.getId();
        productName = item.getProductName();
        quantity = item.getQuantity();
        notes = item.getNotes();
        status = PreparationStatus.PENDING;
    }

    void changeStatus(PreparationStatus status) {
        this.status = status;
        Instant now = Instant.now();
        switch (status) {
            case IN_PREPARATION -> startedAt = now;
            case READY -> {
                if (startedAt == null) {
                    startedAt = now;
                }
                readyAt = now;
            }
            case DELIVERED -> deliveredAt = now;
            case CANCELLED -> cancelledAt = now;
            case PENDING -> {
            }
        }
    }

    Long getId() {
        return id;
    }

    Long getTicketId() {
        return ticketId;
    }

    Long getOrderItemId() {
        return orderItemId;
    }

    String getProductName() {
        return productName;
    }

    int getQuantity() {
        return quantity;
    }

    String getNotes() {
        return notes;
    }

    PreparationStatus getStatus() {
        return status;
    }

    Instant getStartedAt() {
        return startedAt;
    }

    Instant getReadyAt() {
        return readyAt;
    }

    Instant getDeliveredAt() {
        return deliveredAt;
    }

    Instant getCancelledAt() {
        return cancelledAt;
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
}
