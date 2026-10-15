package com.rest.restaurantsystem.structure;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

interface OperationalAreaRepository extends JpaRepository<OperationalArea, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select area from OperationalArea area where area.id = :id")
    Optional<OperationalArea> findLockedById(@Param("id") Long id);

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
