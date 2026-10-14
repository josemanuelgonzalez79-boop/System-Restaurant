package com.rest.restaurantsystem.structure;

import com.rest.restaurantsystem.user.UserResponse;
import com.rest.restaurantsystem.user.UserRole;
import com.rest.restaurantsystem.user.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BranchAssignmentServiceTest {

    @Mock BranchService branchService;
    @Mock UserService userService;
    @InjectMocks BranchAssignmentService service;

    @Test
    void newActiveUsersCanOperateWithoutManualAssignments() {
        when(branchService.findById(1L)).thenReturn(branch());
        when(userService.findById(2L)).thenReturn(user(2L, true));
        when(userService.findAll()).thenReturn(List.of(user(2L, true), user(3L, false)));

        assertTrue(service.isAssigned(1L, 2L));
        assertEquals(List.of(2L), service.findAssignedActiveUsers(1L).stream()
                .map(BranchAssignmentUserResponse::userId).toList());
    }

    @Test
    void inactiveUserCannotOperate() {
        when(branchService.findById(1L)).thenReturn(branch());
        when(userService.findById(3L)).thenReturn(user(3L, false));
        assertFalse(service.isAssigned(1L, 3L));
    }

    private BranchResponse branch() {
        Instant now = Instant.now();
        return new BranchResponse(1L, "PRINCIPAL", "Restaurante", null, null,
                "America/Mazatlan", 0, true, 0L, now, now);
    }

    private UserResponse user(Long id, boolean active) {
        Instant now = Instant.now();
        return new UserResponse(id, "usuario" + id, "Usuario " + id,
                UserRole.OPERATOR, active, false, 0L, now, now);
    }
}
