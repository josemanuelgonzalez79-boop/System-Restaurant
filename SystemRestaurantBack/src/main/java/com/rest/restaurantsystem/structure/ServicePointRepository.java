package com.rest.restaurantsystem.structure;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

interface ServicePointRepository extends JpaRepository<ServicePoint, Long> {

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
