package com.rest.restaurantsystem.structure;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

interface OperationalAreaRepository extends JpaRepository<OperationalArea, Long> {

    @Query("""
            select area
            from OperationalArea area
            join fetch area.branch branch
            order by branch.sortOrder, branch.name, area.sortOrder, area.name
            """)
    List<OperationalArea> findAllOrdered();

    List<OperationalArea> findAllByBranch_IdOrderBySortOrderAscNameAsc(Long branchId);

    boolean existsByBranch_IdAndNameIgnoreCase(Long branchId, String name);

    boolean existsByBranch_IdAndNameIgnoreCaseAndIdNot(Long branchId, String name, Long id);
}
