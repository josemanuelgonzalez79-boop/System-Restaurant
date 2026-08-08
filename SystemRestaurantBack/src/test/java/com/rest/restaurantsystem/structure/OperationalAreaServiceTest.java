package com.rest.restaurantsystem.structure;

import com.rest.restaurantsystem.exception.BadRequestException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OperationalAreaServiceTest {

    @Mock
    private OperationalAreaRepository repository;

    @Mock
    private BranchService branchService;

    @InjectMocks
    private OperationalAreaService service;

    @Test
    void rejectsAreaCreationForInactiveBranch() {
        Branch branch = new Branch(new BranchRequest(
                "PRINCIPAL",
                "Principal",
                null,
                null,
                "America/Mazatlan",
                0
        ));
        branch.setActive(false);
        when(branchService.getEntity(1L)).thenReturn(branch);

        OperationalAreaRequest request = new OperationalAreaRequest(
                1L,
                "Cocina",
                null,
                AreaType.PRODUCTION,
                0
        );

        assertThrows(BadRequestException.class, () -> service.create(request));
    }
}
