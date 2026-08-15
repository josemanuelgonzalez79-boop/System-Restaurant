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
@Table(name = "restaurant_orders")
class RestaurantOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "branch_id", nullable = false)
    private Long branchId;

    @Column(name = "service_point_id")
    private Long servicePointId;

    @Column(name = "assigned_user_id", nullable = false)
    private Long assignedUserId;

    @Column(name = "opened_by_user_id", nullable = false, updatable = false)
    private Long openedByUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "service_mode", nullable = false, length = 30)
    private ServiceMode serviceMode;

    @Column(name = "guest_count", nullable = false)
    private int guestCount;

    @Column(name = "customer_reference", length = 120)
    private String customerReference;

    @Column(length = 500)
    private String notes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private OrderStatus status;

    @Column(name = "opened_at", nullable = false, updatable = false)
    private Instant openedAt;

    @Column(name = "closed_at")
    private Instant closedAt;

    @Version
    private long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected RestaurantOrder() {
    }

    RestaurantOrder(OrderCreateRequest request, Long openedByUserId) {
        branchId = request.branchId();
        servicePointId = request.servicePointId();
        assignedUserId = request.assignedUserId();
        this.openedByUserId = openedByUserId;
        serviceMode = request.serviceMode();
        guestCount = request.guestCount();
        customerReference = normalizeOptional(request.customerReference());
        notes = normalizeOptional(request.notes());
        status = OrderStatus.OPEN;
    }

    void changeStatus(OrderStatus status) {
        this.status = status;
        closedAt = status == OrderStatus.COMPLETED || status == OrderStatus.CANCELLED
                ? Instant.now()
                : null;
    }

    void touch() {
        updatedAt = Instant.now();
    }

    Long getId() {
        return id;
    }

    Long getBranchId() {
        return branchId;
    }

    Long getServicePointId() {
        return servicePointId;
    }

    Long getAssignedUserId() {
        return assignedUserId;
    }

    Long getOpenedByUserId() {
        return openedByUserId;
    }

    ServiceMode getServiceMode() {
        return serviceMode;
    }

    int getGuestCount() {
        return guestCount;
    }

    String getCustomerReference() {
        return customerReference;
    }

    String getNotes() {
        return notes;
    }

    OrderStatus getStatus() {
        return status;
    }

    long getVersion() {
        return version;
    }

    Instant getOpenedAt() {
        return openedAt;
    }

    Instant getUpdatedAt() {
        return updatedAt;
    }

    Instant getClosedAt() {
        return closedAt;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        openedAt = now;
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
