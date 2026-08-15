package com.rest.restaurantsystem.structure;

import com.rest.restaurantsystem.cash.CashRegisterOccupancyService;
import com.rest.restaurantsystem.exception.BadRequestException;
import com.rest.restaurantsystem.exception.ConflictException;
import com.rest.restaurantsystem.exception.ResourceNotFoundException;
import com.rest.restaurantsystem.order.OrderOccupancyService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.List;

@Service
public class BranchService {

    private final BranchRepository repository;
    private final OrderOccupancyService occupancyService;
    private final CashRegisterOccupancyService cashRegisterOccupancyService;

    public BranchService(
            BranchRepository repository,
            OrderOccupancyService occupancyService,
            CashRegisterOccupancyService cashRegisterOccupancyService
    ) {
        this.repository = repository;
        this.occupancyService = occupancyService;
        this.cashRegisterOccupancyService = cashRegisterOccupancyService;
    }

    @Transactional(readOnly = true)
    public List<BranchResponse> findAll() {
        return repository.findAllByOrderBySortOrderAscNameAsc()
                .stream()
                .map(BranchResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public BranchResponse findById(Long id) {
        return BranchResponse.from(getEntity(id));
    }

    @Transactional
    public BranchResponse create(BranchRequest request) {
        validateTimezone(request.timezone());
        String code = request.code().trim();
        if (repository.existsByCodeIgnoreCase(code)) {
            throw new ConflictException("Ya existe una sucursal con esa clave.");
        }
        return BranchResponse.from(repository.save(new Branch(request)));
    }

    @Transactional
    public BranchResponse update(Long id, BranchRequest request) {
        validateTimezone(request.timezone());
        String code = request.code().trim();
        if (repository.existsByCodeIgnoreCaseAndIdNot(code, id)) {
            throw new ConflictException("Ya existe otra sucursal con esa clave.");
        }
        Branch branch = getEntity(id);
        branch.update(request);
        return BranchResponse.from(branch);
    }

    @Transactional
    public BranchResponse changeActive(Long id, boolean active) {
        Branch branch = getEntity(id);
        if (!active && branch.isActive() && repository.countByActiveTrue() <= 1) {
            throw new BadRequestException("Debe permanecer al menos una sucursal activa.");
        }
        if (!active && branch.isActive() && occupancyService.hasActiveOrdersForBranch(id)) {
            throw new BadRequestException(
                    "Completa o cancela los pedidos abiertos antes de desactivar la sucursal."
            );
        }
        if (!active && branch.isActive() && cashRegisterOccupancyService.hasOpenSession(id)) {
            throw new BadRequestException(
                    "Cierra la caja de la sucursal antes de desactivarla."
            );
        }
        branch.setActive(active);
        return BranchResponse.from(branch);
    }

    Branch getEntity(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró la sucursal solicitada."));
    }

    private void validateTimezone(String timezone) {
        try {
            ZoneId.of(timezone.trim());
        } catch (DateTimeException exception) {
            throw new BadRequestException("La zona horaria indicada no es válida.");
        }
    }
}
