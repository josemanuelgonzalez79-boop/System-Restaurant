package com.rest.restaurantsystem.order;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.Set;

@Service
public class OrderOccupancyService {

    private static final Set<OrderStatus> ACTIVE_STATUSES =
            EnumSet.of(OrderStatus.OPEN, OrderStatus.IN_PROGRESS);

    private final RestaurantOrderRepository repository;

    public OrderOccupancyService(RestaurantOrderRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public boolean hasActiveOrdersForBranch(Long branchId) {
        return repository.existsByBranchIdAndStatusIn(branchId, ACTIVE_STATUSES);
    }

    @Transactional(readOnly = true)
    public boolean hasActiveOrdersForArea(Long areaId) {
        return repository.existsActiveByAreaId(areaId);
    }

    @Transactional(readOnly = true)
    public boolean hasActiveOrderForServicePoint(Long servicePointId) {
        return repository.existsByServicePointIdAndStatusIn(servicePointId, ACTIVE_STATUSES);
    }
}
