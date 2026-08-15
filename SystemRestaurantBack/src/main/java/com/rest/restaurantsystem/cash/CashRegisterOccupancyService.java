package com.rest.restaurantsystem.cash;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CashRegisterOccupancyService {

    private final CashRegisterSessionRepository repository;

    public CashRegisterOccupancyService(CashRegisterSessionRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public boolean hasOpenSession(Long branchId) {
        return repository.findByBranchIdAndStatus(branchId, CashRegisterStatus.OPEN).isPresent();
    }
}
