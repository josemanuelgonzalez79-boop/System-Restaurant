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
class ServicePointServiceTest {

    @Mock
    private ServicePointRepository repository;

    @Mock
    private OperationalAreaService areaService;

    @Mock
    private OrderOccupancyService occupancyService;

    @InjectMocks
    private ServicePointService service;

    @Test
    void rejectsPointCreationForInactiveArea() {
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
                "Salón",
                null,
                AreaType.SERVICE,
                0
        ), branch);
        area.setActive(false);
        when(areaService.getEntity(1L)).thenReturn(area);

        ServicePointRequest request = new ServicePointRequest(
                1L,
                "MESA-01",
                "Mesa 1",
                null,
                ServicePointType.TABLE,
                0
        );

        assertThrows(BadRequestException.class, () -> service.create(request));
    }

    @Test
    void rejectsDeactivationWithActiveOrder() {
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
        ServicePoint point = new ServicePoint(new ServicePointRequest(
                5L,
                "MESA-01",
                "Mesa 1",
                null,
                ServicePointType.TABLE,
                0
        ), area);
        ReflectionTestUtils.setField(point, "id", 20L);
        when(repository.findById(20L)).thenReturn(Optional.of(point));
        when(occupancyService.hasActiveOrderForServicePoint(20L)).thenReturn(true);

        assertThrows(BadRequestException.class, () -> service.changeActive(20L, false));
    }
}
