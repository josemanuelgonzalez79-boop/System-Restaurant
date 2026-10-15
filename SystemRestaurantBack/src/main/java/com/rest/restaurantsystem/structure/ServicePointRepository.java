package com.rest.restaurantsystem.structure;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

interface ServicePointRepository extends JpaRepository<ServicePoint, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select point from ServicePoint point where point.id = :id")
    Optional<ServicePoint> findLockedById(@Param("id") Long id);

    @Query("""
            select point
            from ServicePoint point
            join fetch point.area area
            join fetch area.branch branch
            order by branch.sortOrder, branch.name, area.sortOrder, area.name, point.sortOrder, point.name
            """)
    List<ServicePoint> findAllOrdered();

    List<ServicePoint> findAllByArea_IdOrderBySortOrderAscNameAsc(Long areaId);

    boolean existsByArea_IdAndCodeIgnoreCase(Long areaId, String code);

    boolean existsByArea_IdAndCodeIgnoreCaseAndIdNot(Long areaId, String code, Long id);
}
