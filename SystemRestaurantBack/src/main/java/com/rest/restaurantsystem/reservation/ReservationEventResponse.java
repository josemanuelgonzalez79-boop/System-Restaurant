package com.rest.restaurantsystem.reservation;

import java.time.Instant;

public record ReservationEventResponse(
        Long id, String action, String detail, String actorUsername, Instant occurredAt
) {
    static ReservationEventResponse from(ReservationEvent event) {
        return new ReservationEventResponse(event.getId(), event.getAction(), event.getDetail(),
                event.getActorUsername(), event.getOccurredAt());
    }
}
