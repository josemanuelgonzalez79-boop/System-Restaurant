package com.rest.restaurantsystem.order;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderPaymentRepository extends JpaRepository<OrderPayment, Long> {

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

    @Query("""
            SELECT COALESCE(SUM(payment.amount), 0)
            FROM OrderPayment payment
            WHERE payment.cashRegisterSessionId = :sessionId
              AND payment.status = com.rest.restaurantsystem.order.PaymentStatus.ACTIVE
              AND payment.method = :method
            """)
    BigDecimal sumActiveByCashRegisterAndMethod(
            @Param("sessionId") Long sessionId,
            @Param("method") PaymentMethod method
    );

    @Query(
            value = """
                    SELECT EXISTS (
                        SELECT 1
                        FROM order_payments payment
                        JOIN restaurant_orders orders ON orders.id = payment.order_id
                        WHERE payment.cash_register_session_id = :sessionId
                          AND payment.status = 'ACTIVE'
                          AND orders.status IN ('OPEN', 'IN_PROGRESS')
                    )
                    """,
            nativeQuery = true
    )
    boolean existsUnclosedOrderWithActivePayment(@Param("sessionId") Long sessionId);
}
