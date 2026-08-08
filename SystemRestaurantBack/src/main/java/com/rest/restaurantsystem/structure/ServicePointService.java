package com.rest.restaurantsystem.structure;

import com.rest.restaurantsystem.exception.BadRequestException;
import com.rest.restaurantsystem.exception.ConflictException;
import com.rest.restaurantsystem.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ServicePointService {

    private final ServicePointRepository repository;
    private final OperationalAreaService areaService;

    public ServicePointService(ServicePointRepository repository, OperationalAreaService areaService) {
        this.repository = repository;
        this.areaService = areaService;
    }

    @Transactional(readOnly = true)
    public List<ServicePointResponse> findAll(Long areaId) {
        List<ServicePoint> points = areaId == null
                ? repository.findAllOrdered()
                : repository.findAllByArea_IdOrderBySortOrderAscNameAsc(areaId);
        return points.stream().map(ServicePointResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public ServicePointResponse findById(Long id) {
        return ServicePointResponse.from(getEntity(id));
    }

    @Transactional
    public ServicePointResponse create(ServicePointRequest request) {
        OperationalArea area = areaService.getEntity(request.areaId());
        ensureActiveHierarchy(area);
        if (repository.existsByArea_IdAndCodeIgnoreCase(area.getId(), request.code().trim())) {
            throw new ConflictException("Ya existe un punto con esa clave en el área.");
        }
        return ServicePointResponse.from(repository.save(new ServicePoint(request, area)));
    }

    @Transactional
    public ServicePointResponse update(Long id, ServicePointRequest request) {
        OperationalArea area = areaService.getEntity(request.areaId());
        ensureActiveHierarchy(area);
        if (repository.existsByArea_IdAndCodeIgnoreCaseAndIdNot(
                area.getId(),
                request.code().trim(),
                id
        )) {
            throw new ConflictException("Ya existe otro punto con esa clave en el área.");
        }
        ServicePoint point = getEntity(id);
        point.update(request, area);
        return ServicePointResponse.from(point);
    }

    @Transactional
    public ServicePointResponse changeActive(Long id, boolean active) {
        ServicePoint point = getEntity(id);
        if (active) {
            ensureActiveHierarchy(point.getArea());
        }
        point.setActive(active);
        return ServicePointResponse.from(point);
    }

    private ServicePoint getEntity(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró el punto solicitado."));
    }

    private void ensureActiveHierarchy(OperationalArea area) {
        if (!area.getBranch().isActive()) {
            throw new BadRequestException("La sucursal del punto debe estar activa.");
        }
        if (!area.isActive()) {
            throw new BadRequestException("El área debe estar activa para administrar sus puntos.");
        }
    }
}
