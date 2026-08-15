package com.rest.restaurantsystem.order;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

interface OrderItemModifierRepository extends JpaRepository<OrderItemModifier, Long> {

    List<OrderItemModifier> findByOrderItemIdInOrderByOrderItemIdAscIdAsc(
            Collection<Long> orderItemIds
    );

    void deleteByOrderItemId(Long orderItemId);
}
