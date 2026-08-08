package com.rest.restaurantsystem.order;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

interface RestaurantOrderRepository extends JpaRepository<RestaurantOrder, Long> {

    List<RestaurantOrder> findAllByOrderByOpenedAtDesc();

    List<RestaurantOrder> findAllByBranchIdOrderByOpenedAtDesc(Long branchId);

    List<RestaurantOrder> findAllByStatusInOrderByOpenedAtDesc(Collection<OrderStatus> statuses);

    List<RestaurantOrder> findAllByBranchIdAndStatusInOrderByOpenedAtDesc(
            Long branchId,
            Collection<OrderStatus> statuses
    );

    boolean existsByServicePointIdAndStatusIn(Long servicePointId, Collection<OrderStatus> statuses);

    boolean existsByBranchIdAndStatusIn(Long branchId, Collection<OrderStatus> statuses);

    @Query(
            value = """
                    SELECT EXISTS (
                        SELECT 1
                        FROM restaurant_orders orders
                        JOIN service_points point ON point.id = orders.service_point_id
                        WHERE point.area_id = :areaId
                          AND orders.status IN ('OPEN', 'IN_PROGRESS')
                    )
                    """,
            nativeQuery = true
    )
    boolean existsActiveByAreaId(@Param("areaId") Long areaId);
}
