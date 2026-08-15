package com.rest.restaurantsystem.realtime;

import com.rest.restaurantsystem.structure.BranchAssignmentService;
import com.rest.restaurantsystem.user.UserResponse;
import com.rest.restaurantsystem.user.UserRole;
import com.rest.restaurantsystem.user.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;

import java.security.Principal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RealtimeAuthorizationInterceptorTest {

    private UserService userService;
    private BranchAssignmentService assignmentService;
    private RealtimeAuthorizationInterceptor interceptor;

    @BeforeEach
    void setUp() {
        userService = mock(UserService.class);
        assignmentService = mock(BranchAssignmentService.class);
        interceptor = new RealtimeAuthorizationInterceptor(userService, assignmentService);
    }

    @Test
    void acceptsSubscriptionWhenUserIsAssignedToBranch() {
        when(userService.currentUser("mesero")).thenReturn(user());
        when(assignmentService.isAssigned(7L, 10L)).thenReturn(true);
        Message<byte[]> message = subscription("/topic/branches/7", () -> "mesero");

        Message<?> result = interceptor.preSend(message, mock(org.springframework.messaging.MessageChannel.class));

        assertThat(result).isSameAs(message);
    }

    @Test
    void rejectsSubscriptionToAnotherBranch() {
        when(userService.currentUser("mesero")).thenReturn(user());
        when(assignmentService.isAssigned(9L, 10L)).thenReturn(false);
        Message<byte[]> message = subscription("/topic/branches/9", () -> "mesero");

        assertThrows(
                AccessDeniedException.class,
                () -> interceptor.preSend(
                        message,
                        mock(org.springframework.messaging.MessageChannel.class)
                )
        );
    }

    @Test
    void rejectsClientPublishingToBroker() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SEND);
        accessor.setDestination("/topic/branches/7");
        accessor.setUser(() -> "mesero");
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        assertThrows(
                AccessDeniedException.class,
                () -> interceptor.preSend(
                        message,
                        mock(org.springframework.messaging.MessageChannel.class)
                )
        );
    }

    private Message<byte[]> subscription(String destination, Principal principal) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination(destination);
        accessor.setSubscriptionId("test-subscription");
        accessor.setUser(principal);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    private UserResponse user() {
        return new UserResponse(
                10L,
                "mesero",
                "Mesero",
                UserRole.OPERATOR,
                true,
                false,
                0,
                Instant.EPOCH,
                Instant.EPOCH
        );
    }
}
