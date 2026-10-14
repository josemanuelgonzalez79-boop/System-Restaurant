package com.rest.restaurantsystem.inventory;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

interface InventoryCountLineRepository extends JpaRepository<InventoryCountLine, Long> {
    List<InventoryCountLine> findByCountIdOrderByIngredientIdAsc(Long countId);
}
