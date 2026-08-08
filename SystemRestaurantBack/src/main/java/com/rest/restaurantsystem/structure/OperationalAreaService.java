package com.rest.restaurantsystem.structure;

import com.rest.restaurantsystem.exception.BadRequestException;
import com.rest.restaurantsystem.exception.ConflictException;
import com.rest.restaurantsystem.exception.ResourceNotFoundException;
import com.rest.restaurantsystem.order.OrderOccupancyService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class OperationalAreaService {

    private final OperationalAreaRepository repository;
    private final BranchService branchService;
    private final OrderOccupancyService occupancyService;

    public OperationalAreaService(
            OperationalAreaRepository repository,
            BranchService branchService,
            OrderOccupancyService occupancyService
    ) {
        this.repository = repository;
        this.branchService = branchService;
        this.occupancyService = occupancyService;
    }

    @Transactional(readOnly = true)
    public List<OperationalAreaResponse> findAll(Long branchId) {
        List<OperationalArea> areas = branchId == null
                ? repository.findAllOrdered()
                : repository.findAllByBranch_IdOrderBySortOrderAscNameAsc(branchId);
        return areas.stream().map(OperationalAreaResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public OperationalAreaResponse findById(Long id) {
        return OperationalAreaResponse.from(getEntity(id));
    }

    @Transactional
    public OperationalAreaResponse create(OperationalAreaRequest request) {
        Branch branch = branchService.getEntity(request.branchId());
        ensureActiveBranch(branch);
        if (repository.existsByBranch_IdAndNameIgnoreCase(branch.getId(), request.name().trim())) {
            throw new ConflictException("Ya existe un área con ese nombre en la sucursal.");
        }
        return OperationalAreaResponse.from(repository.save(new OperationalArea(request, branch)));
    }

    @Transactional
    public OperationalAreaResponse update(Long id, OperationalAreaRequest request) {
        OperationalArea area = getEntity(id);
        if (!area.getBranch().getId().equals(request.branchId())
                && occupancyService.hasActiveOrdersForArea(id)) {
            throw new BadRequestException(
                    "Completa o cancela los pedidos abiertos antes de mover el área."
            );
        }
        Branch branch = branchService.getEntity(request.branchId());
        ensureActiveBranch(branch);
        if (repository.existsByBranch_IdAndNameIgnoreCaseAndIdNot(
                branch.getId(),
                request.name().trim(),
                id
        )) {
            throw new ConflictException("Ya existe otra área con ese nombre en la sucursal.");
        }
        area.update(request, branch);
        return OperationalAreaResponse.from(area);
    }

    @Transactional
    public OperationalAreaResponse changeActive(Long id, boolean active) {
        OperationalArea area = getEntity(id);
        if (active) {
            ensureActiveBranch(area.getBranch());
        }
        if (!active && area.isActive() && occupancyService.hasActiveOrdersForArea(id)) {
            throw new BadRequestException(
                    "Completa o cancela los pedidos abiertos antes de desactivar el área."
            );
        }
        area.setActive(active);
        return OperationalAreaResponse.from(area);
    }

    OperationalArea getEntity(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró el área solicitada."));
    }

    private void ensureActiveBranch(Branch branch) {
        if (!branch.isActive()) {
            throw new BadRequestException("La sucursal debe estar activa para administrar sus áreas.");
        }
    }
}
