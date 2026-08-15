package com.rest.restaurantsystem.realtime;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
public class RealtimeEventPublisher {

    private final ApplicationEventPublisher applicationEventPublisher;

    public RealtimeEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
    }

    public void publish(
            RealtimeEventType type,
            Long branchId,
            Long orderId,
            Long ticketId,
            Long preparationItemId
    ) {
        applicationEventPublisher.publishEvent(
                RealtimeEvent.create(type, branchId, orderId, ticketId, preparationItemId)
        );
    }
}
