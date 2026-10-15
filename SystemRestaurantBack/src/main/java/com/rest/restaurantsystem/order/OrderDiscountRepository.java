package com.rest.restaurantsystem.order;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface OrderDiscountRepository extends JpaRepository<OrderDiscount, Long> {
    List<OrderDiscount> findByOrderIdOrderByAppliedAtAscIdAsc(Long orderId);
    Optional<OrderDiscount> findByOrderIdAndStatus(Long orderId, OrderDiscountStatus status);
    Optional<OrderDiscount> findByOrderIdAndOperationId(Long orderId, UUID operationId);
    boolean existsByOrderIdAndStatus(Long orderId, OrderDiscountStatus status);
}
