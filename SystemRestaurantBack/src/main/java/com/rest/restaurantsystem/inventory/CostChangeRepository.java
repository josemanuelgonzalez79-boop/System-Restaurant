package com.rest.restaurantsystem.inventory;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

interface CostChangeRepository extends JpaRepository<CostChange, Long> {
    List<CostChange> findTop20ByIngredientIdOrderByChangedAtDescIdDesc(Long ingredientId);
}
