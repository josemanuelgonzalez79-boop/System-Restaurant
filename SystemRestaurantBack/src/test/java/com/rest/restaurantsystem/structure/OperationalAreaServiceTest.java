package com.rest.restaurantsystem.structure;

import com.rest.restaurantsystem.exception.BadRequestException;
import com.rest.restaurantsystem.order.OrderOccupancyService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OperationalAreaServiceTest {

    @Mock
    private OperationalAreaRepository repository;

    @Mock
    private BranchService branchService;

    @Mock
    private OrderOccupancyService occupancyService;

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

    @Test
    void rejectsDeactivationWithActiveOrders() {
        Branch branch = new Branch(new BranchRequest(
                "PRINCIPAL",
                "Principal",
                null,
                null,
                "America/Mazatlan",
                0
        ));
        OperationalArea area = new OperationalArea(new OperationalAreaRequest(
                1L,
                "Comedor",
                null,
                AreaType.SERVICE,
                0
        ), branch);
        ReflectionTestUtils.setField(area, "id", 5L);
        when(repository.findById(5L)).thenReturn(Optional.of(area));
        when(occupancyService.hasActiveOrdersForArea(5L)).thenReturn(true);

        assertThrows(BadRequestException.class, () -> service.changeActive(5L, false));
    }
}
