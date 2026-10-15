package com.rest.restaurantsystem.reservation;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "reservation_events")
class ReservationEvent {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "reservation_id", nullable = false, updatable = false)
    private Long reservationId;
    @Column(nullable = false, length = 20, updatable = false)
    private String action;
    @Column(nullable = false, length = 250, updatable = false)
    private String detail;
    @Column(name = "actor_user_id", nullable = false, updatable = false)
    private Long actorUserId;
    @Column(name = "actor_username", nullable = false, length = 80, updatable = false)
    private String actorUsername;
    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt;

    protected ReservationEvent() { }
    ReservationEvent(Long reservationId, String action, String detail, Long actorUserId, String actorUsername) {
        this.reservationId = reservationId;
        this.action = action;
        this.detail = detail;
        this.actorUserId = actorUserId;
        this.actorUsername = actorUsername;
    }
    Long getId() { return id; }
    String getAction() { return action; }
    String getDetail() { return detail; }
    String getActorUsername() { return actorUsername; }
    Instant getOccurredAt() { return occurredAt; }
    @PrePersist void onCreate() { occurredAt = Instant.now(); }
}
