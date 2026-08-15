package com.rest.restaurantsystem.realtime;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
class RealtimeEventRelay {

    private final SimpMessagingTemplate messagingTemplate;

    RealtimeEventRelay(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    void relay(RealtimeEvent event) {
        messagingTemplate.convertAndSend(
                "/topic/branches/" + event.branchId(),
                event
        );
    }
}
