package com.rest.restaurantsystem.customer;

import java.time.Instant;

public record CustomerVisitResponse(
        Long reservationId, Long orderId, String status, String serviceMode,
        String servicePointName, Instant occurredAt
) { }
