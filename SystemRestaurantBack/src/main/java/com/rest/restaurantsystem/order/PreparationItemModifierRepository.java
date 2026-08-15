package com.rest.restaurantsystem.order;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

interface PreparationItemModifierRepository
        extends JpaRepository<PreparationItemModifier, Long> {

    List<PreparationItemModifier> findByPreparationItemIdInOrderByPreparationItemIdAscSortOrderAscIdAsc(
            Collection<Long> preparationItemIds
    );
}
