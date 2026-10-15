package com.rest.restaurantsystem.reservation;

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
@Table(name = "reservations")
class Reservation {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "branch_id", nullable = false)
    private Long branchId;
    @Column(name = "service_point_id", nullable = false)
    private Long servicePointId;
    @Column(name = "customer_id")
    private Long customerId;
    @Column(name = "guest_name", nullable = false, length = 120)
    private String guestName;
    @Column(name = "guest_phone", length = 30)
    private String guestPhone;
    @Column(name = "guest_count", nullable = false)
    private int guestCount;
    @Column(length = 500)
    private String notes;
    @Column(name = "start_at", nullable = false)
    private Instant startAt;
    @Column(name = "end_at", nullable = false)
    private Instant endAt;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReservationStatus status;
    @Column(name = "order_id")
    private Long orderId;
    @Column(name = "created_by_user_id", nullable = false, updatable = false)
    private Long createdByUserId;
    @Column(name = "created_by_username", nullable = false, length = 80, updatable = false)
    private String createdByUsername;
    @Column(name = "updated_by_username", nullable = false, length = 80)
    private String updatedByUsername;
    @Column(name = "cancel_reason", length = 250)
    private String cancelReason;
    @Version
    private long version;
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Reservation() { }

    Reservation(Long branchId, ReservationSaveRequest request, Instant start, Instant end,
                Long userId, String username) {
        this.branchId = branchId;
        this.status = ReservationStatus.BOOKED;
        this.createdByUserId = userId;
        this.createdByUsername = username;
        reschedule(request, start, end, username);
    }

    void reschedule(ReservationSaveRequest request, Instant start, Instant end, String username) {
        servicePointId = request.servicePointId();
        customerId = request.customerId();
        guestName = request.guestName().trim();
        guestPhone = normalize(request.guestPhone());
        guestCount = request.guestCount();
        notes = normalize(request.notes());
        startAt = start;
        endAt = end;
        updatedByUsername = username;
    }

    void cancel(String reason, String username) {
        status = ReservationStatus.CANCELLED;
        cancelReason = reason.trim();
        updatedByUsername = username;
    }

    void markNoShow(String username) {
        status = ReservationStatus.NO_SHOW;
        updatedByUsername = username;
    }

    void seat(Long orderId, String username) {
        status = ReservationStatus.SEATED;
        this.orderId = orderId;
        updatedByUsername = username;
    }

    Long getId() { return id; }
    Long getBranchId() { return branchId; }
    Long getServicePointId() { return servicePointId; }
    Long getCustomerId() { return customerId; }
    String getGuestName() { return guestName; }
    String getGuestPhone() { return guestPhone; }
    int getGuestCount() { return guestCount; }
    String getNotes() { return notes; }
    Instant getStartAt() { return startAt; }
    Instant getEndAt() { return endAt; }
    ReservationStatus getStatus() { return status; }
    Long getOrderId() { return orderId; }
    String getCreatedByUsername() { return createdByUsername; }
    String getUpdatedByUsername() { return updatedByUsername; }
    String getCancelReason() { return cancelReason; }
    long getVersion() { return version; }
    Instant getCreatedAt() { return createdAt; }
    Instant getUpdatedAt() { return updatedAt; }

    @PrePersist
    void onCreate() { createdAt = Instant.now(); updatedAt = createdAt; }
    @PreUpdate
    void onUpdate() { updatedAt = Instant.now(); }

    private String normalize(String value) { return value == null || value.isBlank() ? null : value.trim(); }
}
