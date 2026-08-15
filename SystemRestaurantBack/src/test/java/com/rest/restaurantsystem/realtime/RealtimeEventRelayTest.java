package com.rest.restaurantsystem.realtime;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class RealtimeEventRelayTest {

    @Test
    void relaysCommittedEventOnlyToItsBranchTopic() {
        SimpMessagingTemplate messagingTemplate = mock(SimpMessagingTemplate.class);
        RealtimeEventRelay relay = new RealtimeEventRelay(messagingTemplate);
        RealtimeEvent event = new RealtimeEvent(
                UUID.randomUUID(),
                RealtimeEventType.PREPARATION_DISPATCHED,
                7L,
                42L,
                null,
                null,
                Instant.EPOCH
        );

        relay.relay(event);

        ArgumentCaptor<Object> payload = ArgumentCaptor.forClass(Object.class);
        verify(messagingTemplate).convertAndSend(
                org.mockito.ArgumentMatchers.eq("/topic/branches/7"),
                payload.capture()
        );
        assertThat(payload.getValue()).isSameAs(event);
    }
}
