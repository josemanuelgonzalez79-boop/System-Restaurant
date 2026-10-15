package com.rest.restaurantsystem.customer;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "restaurant_customer_events")
class CustomerEvent {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "customer_id", nullable = false)
    private Long customerId;
    @Column(nullable = false, length = 20)
    private String action;
    @Column(name = "actor_username", nullable = false, length = 80)
    private String actorUsername;
    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    protected CustomerEvent() { }
    CustomerEvent(Long customerId, String action, String actorUsername) {
        this.customerId = customerId;
        this.action = action;
        this.actorUsername = actorUsername;
    }
    @PrePersist void onCreate() { occurredAt = Instant.now(); }
    Long getId() { return id; }
    String getAction() { return action; }
    String getActorUsername() { return actorUsername; }
    Instant getOccurredAt() { return occurredAt; }
}
