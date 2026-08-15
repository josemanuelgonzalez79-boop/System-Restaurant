package com.rest.restaurantsystem.order;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface OrderPaymentRepository extends JpaRepository<OrderPayment, Long> {

    List<OrderPayment> findByOrderIdOrderByReceivedAtAscIdAsc(Long orderId);

    Optional<OrderPayment> findByOrderIdAndOperationId(Long orderId, UUID operationId);

    boolean existsByOrderIdAndStatus(Long orderId, PaymentStatus status);

    @Query(
            value = """
                    SELECT COALESCE(SUM(amount), 0)
                    FROM order_payments
                    WHERE order_id = :orderId
                      AND status = 'ACTIVE'
                    """,
            nativeQuery = true
    )
    BigDecimal sumActiveByOrderId(@Param("orderId") Long orderId);
}
