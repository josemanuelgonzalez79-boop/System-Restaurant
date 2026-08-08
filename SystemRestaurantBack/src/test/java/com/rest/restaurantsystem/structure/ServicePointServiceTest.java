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
class ServicePointServiceTest {

    @Mock
    private ServicePointRepository repository;

    @Mock
    private OperationalAreaService areaService;

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
}
