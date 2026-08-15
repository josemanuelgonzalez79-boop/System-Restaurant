package com.rest.restaurantsystem.catalog.modifier;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

interface ModifierOptionRepository extends JpaRepository<ModifierOption, Long> {

    List<ModifierOption> findAllByOrderByGroupIdAscSortOrderAscNameAsc();

    List<ModifierOption> findByGroupIdOrderBySortOrderAscNameAsc(Long groupId);

    List<ModifierOption> findByGroupIdInOrderByGroupIdAscSortOrderAscNameAsc(Collection<Long> groupIds);

    boolean existsByGroupIdAndNameIgnoreCase(Long groupId, String name);

    boolean existsByGroupIdAndNameIgnoreCaseAndIdNot(Long groupId, String name, Long id);
}
