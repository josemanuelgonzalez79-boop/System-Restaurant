package com.rest.restaurantsystem.structure;

import com.rest.restaurantsystem.exception.BadRequestException;
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

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BranchAssignmentServiceTest {

    @Mock
    private BranchUserAssignmentRepository repository;

    @Mock
    private BranchService branchService;

    @Mock
    private UserService userService;

    @InjectMocks
    private BranchAssignmentService service;

    @Test
    void requiresAnOwnerInEveryAssignment() {
        Branch branch = new Branch(new BranchRequest(
                "PRINCIPAL",
                "Principal",
                null,
                null,
                "America/Mazatlan",
                0
        ));
        when(branchService.getEntity(1L)).thenReturn(branch);
        when(userService.findAll()).thenReturn(List.of(user(2L, UserRole.OPERATOR)));

        BranchAssignmentRequest request = new BranchAssignmentRequest(List.of(2L));

        assertThrows(BadRequestException.class, () -> service.replace(1L, request));
        verify(repository, never()).deleteAllByBranchId(1L);
    }

    private UserResponse user(Long id, UserRole role) {
        Instant now = Instant.now();
        return new UserResponse(
                id,
                "usuario" + id,
                "Usuario " + id,
                role,
                true,
                false,
                0,
                now,
                now
        );
    }
}
