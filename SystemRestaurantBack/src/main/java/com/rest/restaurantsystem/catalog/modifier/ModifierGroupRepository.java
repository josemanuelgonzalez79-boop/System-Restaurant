package com.rest.restaurantsystem.catalog.modifier;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

interface ModifierGroupRepository extends JpaRepository<ModifierGroup, Long> {

    List<ModifierGroup> findAllByOrderByProductIdAscSortOrderAscNameAsc();

    List<ModifierGroup> findByProductIdOrderBySortOrderAscNameAsc(Long productId);

    boolean existsByProductIdAndNameIgnoreCase(Long productId, String name);

    boolean existsByProductIdAndNameIgnoreCaseAndIdNot(Long productId, String name, Long id);
}
