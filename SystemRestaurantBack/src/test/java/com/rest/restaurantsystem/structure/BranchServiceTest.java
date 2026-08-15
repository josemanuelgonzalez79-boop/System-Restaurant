package com.rest.restaurantsystem.structure;

import com.rest.restaurantsystem.cash.CashRegisterOccupancyService;
import com.rest.restaurantsystem.exception.BadRequestException;
import com.rest.restaurantsystem.order.OrderOccupancyService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BranchServiceTest {

    @Mock
    private BranchRepository repository;

    @Mock
    private OrderOccupancyService occupancyService;

    @Mock
    private CashRegisterOccupancyService cashRegisterOccupancyService;

    @InjectMocks
    private BranchService service;

    @Test
    void rejectsInvalidTimezone() {
        BranchRequest request = new BranchRequest(
                "CENTRO",
                "Sucursal Centro",
                null,
                null,
                "Zona/Inexistente",
                0
        );

        assertThrows(BadRequestException.class, () -> service.create(request));
    }

    @Test
    void keepsAtLeastOneActiveBranch() {
        Branch branch = new Branch(validRequest());
        when(repository.findById(1L)).thenReturn(Optional.of(branch));
        when(repository.countByActiveTrue()).thenReturn(1L);

        assertThrows(BadRequestException.class, () -> service.changeActive(1L, false));
    }

    @Test
    void rejectsDeactivationWithActiveOrders() {
        Branch branch = new Branch(validRequest());
        when(repository.findById(1L)).thenReturn(Optional.of(branch));
        when(repository.countByActiveTrue()).thenReturn(2L);
        when(occupancyService.hasActiveOrdersForBranch(1L)).thenReturn(true);

        assertThrows(BadRequestException.class, () -> service.changeActive(1L, false));
    }

    @Test
    void rejectsDeactivationWithOpenCashRegister() {
        Branch branch = new Branch(validRequest());
        when(repository.findById(1L)).thenReturn(Optional.of(branch));
        when(repository.countByActiveTrue()).thenReturn(2L);
        when(cashRegisterOccupancyService.hasOpenSession(1L)).thenReturn(true);

        assertThrows(BadRequestException.class, () -> service.changeActive(1L, false));
    }

    private BranchRequest validRequest() {
        return new BranchRequest(
                "PRINCIPAL",
                "Principal",
                null,
                null,
                "America/Mazatlan",
                0
        );
    }
}
