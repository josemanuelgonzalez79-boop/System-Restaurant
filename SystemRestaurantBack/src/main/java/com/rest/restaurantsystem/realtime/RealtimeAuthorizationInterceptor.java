package com.rest.restaurantsystem.realtime;

import com.rest.restaurantsystem.structure.BranchAssignmentService;
import com.rest.restaurantsystem.user.UserResponse;
import com.rest.restaurantsystem.user.UserService;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
class RealtimeAuthorizationInterceptor implements ChannelInterceptor {

    private static final Pattern BRANCH_TOPIC = Pattern.compile("^/topic/branches/(\\d+)$");

    private final UserService userService;
    private final BranchAssignmentService assignmentService;

    RealtimeAuthorizationInterceptor(
            UserService userService,
            BranchAssignmentService assignmentService
    ) {
        this.userService = userService;
        this.assignmentService = assignmentService;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
        StompCommand command = accessor.getCommand();
        if (command == StompCommand.CONNECT) {
            requireAuthenticated(accessor.getUser());
        } else if (command == StompCommand.SUBSCRIBE) {
            authorizeSubscription(accessor.getUser(), accessor.getDestination());
        } else if (command == StompCommand.SEND) {
            throw new AccessDeniedException(
                    "Los clientes no pueden publicar eventos operativos."
            );
        }
        return message;
    }

    private void authorizeSubscription(Principal principal, String destination) {
        Principal authenticated = requireAuthenticated(principal);
        Matcher matcher = BRANCH_TOPIC.matcher(destination == null ? "" : destination);
        if (!matcher.matches()) {
            throw new AccessDeniedException("El canal solicitado no está permitido.");
        }
        Long branchId = Long.valueOf(matcher.group(1));
        UserResponse user = userService.currentUser(authenticated.getName());
        if (!user.active() || !assignmentService.isAssigned(branchId, user.id())) {
            throw new AccessDeniedException(
                    "Tu usuario no está asignado a la sucursal solicitada."
            );
        }
    }

    private Principal requireAuthenticated(Principal principal) {
        if (principal == null || principal.getName() == null || principal.getName().isBlank()) {
            throw new AccessDeniedException("Debes iniciar sesión para usar el canal en tiempo real.");
        }
        return principal;
    }
}
