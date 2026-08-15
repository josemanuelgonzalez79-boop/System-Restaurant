package com.rest.restaurantsystem.order;

import com.rest.restaurantsystem.catalog.product.ProductDestination;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "preparation_tickets")
class PreparationTicket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "branch_id", nullable = false)
    private Long branchId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProductDestination destination;

    @Column(name = "sequence_number", nullable = false)
    private int sequenceNumber;

    @Column(name = "sent_by_user_id", nullable = false)
    private Long sentByUserId;

    @Column(name = "sent_at", nullable = false, updatable = false)
    private Instant sentAt;

    protected PreparationTicket() {
    }

    PreparationTicket(
            Long orderId,
            Long branchId,
            ProductDestination destination,
            int sequenceNumber,
            Long sentByUserId
    ) {
        this.orderId = orderId;
        this.branchId = branchId;
        this.destination = destination;
        this.sequenceNumber = sequenceNumber;
        this.sentByUserId = sentByUserId;
    }

    Long getId() {
        return id;
    }

    Long getOrderId() {
        return orderId;
    }

    Long getBranchId() {
        return branchId;
    }

    ProductDestination getDestination() {
        return destination;
    }

    int getSequenceNumber() {
        return sequenceNumber;
    }

    Long getSentByUserId() {
        return sentByUserId;
    }

    Instant getSentAt() {
        return sentAt;
    }

    @PrePersist
    void onCreate() {
        sentAt = Instant.now();
    }
}
