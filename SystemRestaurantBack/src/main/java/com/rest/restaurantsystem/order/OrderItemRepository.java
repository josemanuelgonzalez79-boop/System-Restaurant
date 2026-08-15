package com.rest.restaurantsystem.order;

import org.springframework.data.jpa.repository.JpaRepository;

import com.rest.restaurantsystem.catalog.product.ProductDestination;

import java.util.List;
import java.util.Set;

interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    List<OrderItem> findByOrderIdOrderByCreatedAtAscIdAsc(Long orderId);

    long countByOrderId(Long orderId);

    boolean existsByOrderIdAndSentAtIsNullAndDestinationIn(
            Long orderId,
            Set<ProductDestination> destinations
    );
}
