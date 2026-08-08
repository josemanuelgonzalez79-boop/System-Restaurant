package com.rest.restaurantsystem.structure;

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

import java.time.Instant;

@Entity
@Table(name = "operational_areas")
class OperationalArea {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "branch_id", nullable = false)
    private Branch branch;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(length = 250)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "area_type", nullable = false, length = 30)
    private AreaType areaType;

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

    protected OperationalArea() {
    }

    OperationalArea(OperationalAreaRequest request, Branch branch) {
        active = true;
        apply(request, branch);
    }

    void update(OperationalAreaRequest request, Branch branch) {
        apply(request, branch);
    }

    void setActive(boolean active) {
        this.active = active;
    }

    Long getId() {
        return id;
    }

    Branch getBranch() {
        return branch;
    }

    String getName() {
        return name;
    }

    String getDescription() {
        return description;
    }

    AreaType getAreaType() {
        return areaType;
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

    private void apply(OperationalAreaRequest request, Branch branch) {
        this.branch = branch;
        name = request.name().trim();
        description = normalizeOptional(request.description());
        areaType = request.areaType();
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
