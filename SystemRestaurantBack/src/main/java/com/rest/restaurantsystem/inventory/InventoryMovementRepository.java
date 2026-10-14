package com.rest.restaurantsystem.inventory;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface InventoryMovementRepository extends JpaRepository<InventoryMovement, Long> {

    Optional<InventoryMovement> findByOperationId(UUID operationId);

    boolean existsByIngredientId(Long ingredientId);

    List<InventoryMovement> findTop50ByIngredientIdOrderByOccurredAtDescIdDesc(Long ingredientId);
}
