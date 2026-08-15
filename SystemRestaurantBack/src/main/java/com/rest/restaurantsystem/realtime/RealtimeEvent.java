package com.rest.restaurantsystem.realtime;

import java.time.Instant;
import java.util.UUID;

public record RealtimeEvent(
        UUID eventId,
        RealtimeEventType type,
        Long branchId,
        Long orderId,
        Long ticketId,
        Long preparationItemId,
        Instant occurredAt
) {

    static RealtimeEvent create(
            RealtimeEventType type,
            Long branchId,
            Long orderId,
            Long ticketId,
            Long preparationItemId
    ) {
        return new RealtimeEvent(
                UUID.randomUUID(),
                type,
                branchId,
                orderId,
                ticketId,
                preparationItemId,
                Instant.now()
        );
    }
}
