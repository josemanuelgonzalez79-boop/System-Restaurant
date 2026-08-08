package com.rest.restaurantsystem.structure;

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
import java.util.Locale;

@Entity
@Table(name = "branches")
class Branch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 30)
    private String code;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(length = 250)
    private String address;

    @Column(length = 30)
    private String phone;

    @Column(nullable = false, length = 60)
    private String timezone;

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

    protected Branch() {
    }

    Branch(BranchRequest request) {
        active = true;
        apply(request);
    }

    void update(BranchRequest request) {
        apply(request);
    }

    void setActive(boolean active) {
        this.active = active;
    }

    Long getId() {
        return id;
    }

    String getCode() {
        return code;
    }

    String getName() {
        return name;
    }

    String getAddress() {
        return address;
    }

    String getPhone() {
        return phone;
    }

    String getTimezone() {
        return timezone;
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

    private void apply(BranchRequest request) {
        code = request.code().trim().toUpperCase(Locale.ROOT);
        name = request.name().trim();
        address = normalizeOptional(request.address());
        phone = normalizeOptional(request.phone());
        timezone = request.timezone().trim();
        sortOrder = request.sortOrder();
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
