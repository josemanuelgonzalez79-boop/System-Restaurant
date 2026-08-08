package com.rest.restaurantsystem.structure;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "branch_user_assignments")
class BranchUserAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "branch_id", nullable = false)
    private Long branchId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "assigned_at", nullable = false, updatable = false)
    private Instant assignedAt;

    protected BranchUserAssignment() {
    }

    BranchUserAssignment(Long branchId, Long userId) {
        this.branchId = branchId;
        this.userId = userId;
    }

    Long getUserId() {
        return userId;
    }

    @PrePersist
    void onCreate() {
        assignedAt = Instant.now();
    }
}
