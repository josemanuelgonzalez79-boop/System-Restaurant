package com.rest.restaurantsystem.inventory;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "inventory_counts")
class InventoryCount {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "operation_id", nullable = false, unique = true, updatable = false)
    private UUID operationId;
    @Column(nullable = false, length = 250, updatable = false)
    private String reason;
    @Column(name = "recorded_by_user_id", nullable = false, updatable = false)
    private Long recordedByUserId;
    @Column(name = "recorded_by_username", nullable = false, length = 80, updatable = false)
    private String recordedByUsername;
    @Column(name = "counted_at", nullable = false, updatable = false)
    private Instant countedAt;

    protected InventoryCount() { }

    InventoryCount(UUID operationId, String reason, Long recordedByUserId, String recordedByUsername) {
        this.operationId = operationId;
        this.reason = reason.trim();
        this.recordedByUserId = recordedByUserId;
        this.recordedByUsername = recordedByUsername;
    }

    Long getId() { return id; }
    UUID getOperationId() { return operationId; }
    String getReason() { return reason; }
    String getRecordedByUsername() { return recordedByUsername; }
    Instant getCountedAt() { return countedAt; }

    @PrePersist
    void onCreate() { countedAt = Instant.now(); }
}
