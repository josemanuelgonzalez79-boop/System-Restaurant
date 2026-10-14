package com.rest.restaurantsystem.inventory;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface InventoryCountRepository extends JpaRepository<InventoryCount, Long> {
    Optional<InventoryCount> findByOperationId(UUID operationId);
    List<InventoryCount> findTop20ByOrderByCountedAtDescIdDesc();
}
