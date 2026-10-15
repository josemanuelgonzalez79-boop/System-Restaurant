package com.rest.restaurantsystem.reservation;

import java.time.Instant;

public record ReservationResponse(
        Long id, Long branchId, Long servicePointId, Long customerId,
        String servicePointName, String areaName,
        String guestName, String guestPhone, int guestCount, String notes,
        Instant startAt, Instant endAt, String timezone, ReservationStatus status,
        Long orderId, String createdByUsername, String updatedByUsername,
        String cancelReason, long version, Instant createdAt, Instant updatedAt
) { }
