package com.rest.restaurantsystem.cash;

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
@Table(name = "cash_register_sessions")
class CashRegisterSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "branch_id", nullable = false, updatable = false)
    private Long branchId;

    @Column(name = "opening_amount", nullable = false, precision = 12, scale = 2, updatable = false)
    private BigDecimal openingAmount;

    @Column(name = "opening_notes", length = 250, updatable = false)
    private String openingNotes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CashRegisterStatus status;

    @Column(name = "opened_by_user_id", nullable = false, updatable = false)
    private Long openedByUserId;

    @Column(name = "opened_at", nullable = false, updatable = false)
    private Instant openedAt;

    @Column(name = "closed_by_user_id")
    private Long closedByUserId;

    @Column(name = "closed_at")
    private Instant closedAt;

    @Column(name = "expected_cash_at_close", precision = 12, scale = 2)
    private BigDecimal expectedCashAtClose;

    @Column(name = "counted_cash_at_close", precision = 12, scale = 2)
    private BigDecimal countedCashAtClose;

    @Column(name = "difference_at_close", precision = 12, scale = 2)
    private BigDecimal differenceAtClose;

    @Column(name = "closing_notes", length = 250)
    private String closingNotes;

    @Version
    private long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected CashRegisterSession() {
    }

    CashRegisterSession(
            Long branchId,
            BigDecimal openingAmount,
            String openingNotes,
            Long openedByUserId
    ) {
        this.branchId = branchId;
        this.openingAmount = openingAmount;
        this.openingNotes = normalizeOptional(openingNotes);
        this.openedByUserId = openedByUserId;
        status = CashRegisterStatus.OPEN;
    }

    void touch() {
        updatedAt = Instant.now();
    }

    void close(
            BigDecimal expectedCash,
            BigDecimal countedCash,
            Long closedByUserId,
            String closingNotes
    ) {
        status = CashRegisterStatus.CLOSED;
        expectedCashAtClose = expectedCash;
        countedCashAtClose = countedCash;
        differenceAtClose = countedCash.subtract(expectedCash);
        this.closedByUserId = closedByUserId;
        closedAt = Instant.now();
        this.closingNotes = normalizeOptional(closingNotes);
    }

    Long getId() {
        return id;
    }

    Long getBranchId() {
        return branchId;
    }

    BigDecimal getOpeningAmount() {
        return openingAmount;
    }

    String getOpeningNotes() {
        return openingNotes;
    }

    CashRegisterStatus getStatus() {
        return status;
    }

    Long getOpenedByUserId() {
        return openedByUserId;
    }

    Instant getOpenedAt() {
        return openedAt;
    }

    Long getClosedByUserId() {
        return closedByUserId;
    }

    Instant getClosedAt() {
        return closedAt;
    }

    BigDecimal getExpectedCashAtClose() {
        return expectedCashAtClose;
    }

    BigDecimal getCountedCashAtClose() {
        return countedCashAtClose;
    }

    BigDecimal getDifferenceAtClose() {
        return differenceAtClose;
    }

    String getClosingNotes() {
        return closingNotes;
    }

    long getVersion() {
        return version;
    }

    Instant getUpdatedAt() {
        return updatedAt;
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
