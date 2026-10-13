package com.rest.restaurantsystem.report;

import com.rest.restaurantsystem.order.PaymentMethod;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Repository
public class DailyReportQueryRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public DailyReportQueryRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    List<PaymentMethodRow> findPaymentMethods(Long branchId, Instant start, Instant end) {
        return jdbc.query(
                """
                SELECT payment.method,
                       COUNT(*) FILTER (
                           WHERE payment.received_at >= :startAt
                             AND payment.received_at < :endAt
                       ) AS received_count,
                       COALESCE(SUM(payment.amount) FILTER (
                           WHERE payment.received_at >= :startAt
                             AND payment.received_at < :endAt
                       ), 0) AS gross_amount,
                       COUNT(*) FILTER (
                           WHERE payment.status = 'VOIDED'
                             AND payment.voided_at >= :startAt
                             AND payment.voided_at < :endAt
                       ) AS voided_count,
                       COALESCE(SUM(payment.amount) FILTER (
                           WHERE payment.status = 'VOIDED'
                             AND payment.voided_at >= :startAt
                             AND payment.voided_at < :endAt
                       ), 0) AS voided_amount
                FROM order_payments payment
                JOIN restaurant_orders orders ON orders.id = payment.order_id
                WHERE orders.branch_id = :branchId
                  AND (
                      (payment.received_at >= :startAt AND payment.received_at < :endAt)
                      OR (
                          payment.status = 'VOIDED'
                          AND payment.voided_at >= :startAt
                          AND payment.voided_at < :endAt
                      )
                  )
                GROUP BY payment.method
                """,
                parameters(branchId, start, end),
                (result, rowNumber) -> new PaymentMethodRow(
                        PaymentMethod.valueOf(result.getString("method")),
                        result.getBigDecimal("gross_amount"),
                        result.getBigDecimal("voided_amount"),
                        result.getLong("received_count"),
                        result.getLong("voided_count")
                )
        );
    }

    List<DailyReportResponse.HourlySalesSummary> findHourlySales(
            Long branchId,
            Instant start,
            Instant end,
            String timezone
    ) {
        MapSqlParameterSource parameters = parameters(branchId, start, end)
                .addValue("timezone", timezone);
        return jdbc.query(
                """
                WITH payment_events AS (
                    SELECT EXTRACT(HOUR FROM payment.received_at AT TIME ZONE :timezone)::INTEGER AS hour,
                           payment.amount AS amount,
                           1 AS received_count,
                           0 AS voided_count
                    FROM order_payments payment
                    JOIN restaurant_orders orders ON orders.id = payment.order_id
                    WHERE orders.branch_id = :branchId
                      AND payment.received_at >= :startAt
                      AND payment.received_at < :endAt

                    UNION ALL

                    SELECT EXTRACT(HOUR FROM payment.voided_at AT TIME ZONE :timezone)::INTEGER AS hour,
                           -payment.amount AS amount,
                           0 AS received_count,
                           1 AS voided_count
                    FROM order_payments payment
                    JOIN restaurant_orders orders ON orders.id = payment.order_id
                    WHERE orders.branch_id = :branchId
                      AND payment.status = 'VOIDED'
                      AND payment.voided_at >= :startAt
                      AND payment.voided_at < :endAt
                )
                SELECT hour,
                       COALESCE(SUM(amount), 0) AS net_amount,
                       SUM(received_count) AS received_count,
                       SUM(voided_count) AS voided_count
                FROM payment_events
                GROUP BY hour
                ORDER BY hour
                """,
                parameters,
                (result, rowNumber) -> new DailyReportResponse.HourlySalesSummary(
                        result.getInt("hour"),
                        result.getBigDecimal("net_amount"),
                        result.getLong("received_count"),
                        result.getLong("voided_count")
                )
        );
    }

    OrderMetricsRow findOrderMetrics(Long branchId, Instant start, Instant end) {
        return jdbc.queryForObject(
                """
                WITH modifier_totals AS (
                    SELECT modifier.order_item_id,
                           COALESCE(SUM(modifier.price_delta), 0) AS modifier_total
                    FROM order_item_modifiers modifier
                    GROUP BY modifier.order_item_id
                ),
                completed_order_totals AS (
                    SELECT item.order_id,
                           COALESCE(SUM(
                               item.quantity * (item.unit_price + COALESCE(modifier.modifier_total, 0))
                           ), 0) AS order_total
                    FROM order_items item
                    LEFT JOIN modifier_totals modifier ON modifier.order_item_id = item.id
                    GROUP BY item.order_id
                )
                SELECT COUNT(*) FILTER (
                           WHERE orders.opened_at >= :startAt AND orders.opened_at < :endAt
                       ) AS opened_count,
                       COUNT(*) FILTER (
                           WHERE orders.status = 'COMPLETED'
                             AND orders.closed_at >= :startAt AND orders.closed_at < :endAt
                       ) AS completed_count,
                       COUNT(*) FILTER (
                           WHERE orders.status = 'CANCELLED'
                             AND orders.closed_at >= :startAt AND orders.closed_at < :endAt
                       ) AS cancelled_count,
                       COUNT(*) FILTER (
                           WHERE orders.opened_at < :endAt
                             AND (orders.closed_at IS NULL OR orders.closed_at >= :endAt)
                       ) AS pending_at_end_count,
                       COUNT(*) FILTER (
                           WHERE orders.service_mode = 'DINE_IN'
                             AND orders.opened_at >= :startAt AND orders.opened_at < :endAt
                       ) AS dine_in_count,
                       COUNT(*) FILTER (
                           WHERE orders.service_mode = 'TAKEOUT'
                             AND orders.opened_at >= :startAt AND orders.opened_at < :endAt
                       ) AS takeout_count,
                       COALESCE(SUM(orders.guest_count) FILTER (
                           WHERE orders.opened_at >= :startAt AND orders.opened_at < :endAt
                       ), 0) AS guest_count,
                       COALESCE(SUM(completed.order_total) FILTER (
                           WHERE orders.status = 'COMPLETED'
                             AND orders.closed_at >= :startAt AND orders.closed_at < :endAt
                       ), 0) AS completed_order_value
                FROM restaurant_orders orders
                LEFT JOIN completed_order_totals completed ON completed.order_id = orders.id
                WHERE orders.branch_id = :branchId
                  AND orders.opened_at < :endAt
                """,
                parameters(branchId, start, end),
                (result, rowNumber) -> new OrderMetricsRow(
                        result.getLong("opened_count"),
                        result.getLong("completed_count"),
                        result.getLong("cancelled_count"),
                        result.getLong("pending_at_end_count"),
                        result.getLong("dine_in_count"),
                        result.getLong("takeout_count"),
                        result.getLong("guest_count"),
                        result.getBigDecimal("completed_order_value")
                )
        );
    }

    List<ProductSalesRow> findTopProducts(Long branchId, Instant start, Instant end) {
        return jdbc.query(
                """
                WITH modifier_totals AS (
                    SELECT modifier.order_item_id,
                           COALESCE(SUM(modifier.price_delta), 0) AS modifier_total
                    FROM order_item_modifiers modifier
                    GROUP BY modifier.order_item_id
                )
                SELECT item.product_name,
                       SUM(item.quantity) AS quantity,
                       SUM(item.quantity * (
                           item.unit_price + COALESCE(modifier.modifier_total, 0)
                       )) AS amount
                FROM order_items item
                JOIN restaurant_orders orders ON orders.id = item.order_id
                LEFT JOIN modifier_totals modifier ON modifier.order_item_id = item.id
                WHERE orders.branch_id = :branchId
                  AND orders.status = 'COMPLETED'
                  AND orders.closed_at >= :startAt
                  AND orders.closed_at < :endAt
                GROUP BY item.product_name
                ORDER BY quantity DESC, amount DESC, item.product_name
                LIMIT 10
                """,
                parameters(branchId, start, end),
                (result, rowNumber) -> new ProductSalesRow(
                        result.getString("product_name"),
                        result.getLong("quantity"),
                        result.getBigDecimal("amount")
                )
        );
    }

    CashMetricsRow findCashMetrics(Long branchId, Instant start, Instant end) {
        return jdbc.queryForObject(
                """
                SELECT COUNT(*) FILTER (
                           WHERE session.opened_at >= :startAt AND session.opened_at < :endAt
                       ) AS opened_session_count,
                       COUNT(*) FILTER (
                           WHERE session.closed_at >= :startAt AND session.closed_at < :endAt
                       ) AS closed_session_count,
                       COUNT(*) FILTER (
                           WHERE session.opened_at < :endAt
                             AND (session.closed_at IS NULL OR session.closed_at >= :endAt)
                       ) AS open_at_end_count,
                       COALESCE(SUM(session.opening_amount) FILTER (
                           WHERE session.opened_at >= :startAt AND session.opened_at < :endAt
                       ), 0) AS opening_amount,
                       COALESCE(SUM(session.difference_at_close) FILTER (
                           WHERE session.closed_at >= :startAt AND session.closed_at < :endAt
                       ), 0) AS closing_difference,
                       COALESCE((
                           SELECT SUM(movement.amount)
                           FROM cash_register_movements movement
                           JOIN cash_register_sessions movement_session
                             ON movement_session.id = movement.session_id
                           WHERE movement_session.branch_id = :branchId
                             AND movement.movement_type = 'CASH_IN'
                             AND movement.recorded_at >= :startAt
                             AND movement.recorded_at < :endAt
                       ), 0) AS cash_in_gross,
                       COALESCE((
                           SELECT SUM(movement.amount)
                           FROM cash_register_movements movement
                           JOIN cash_register_sessions movement_session
                             ON movement_session.id = movement.session_id
                           WHERE movement_session.branch_id = :branchId
                             AND movement.movement_type = 'CASH_IN'
                             AND movement.status = 'VOIDED'
                             AND movement.voided_at >= :startAt
                             AND movement.voided_at < :endAt
                       ), 0) AS cash_in_voided,
                       COALESCE((
                           SELECT SUM(movement.amount)
                           FROM cash_register_movements movement
                           JOIN cash_register_sessions movement_session
                             ON movement_session.id = movement.session_id
                           WHERE movement_session.branch_id = :branchId
                             AND movement.movement_type = 'CASH_OUT'
                             AND movement.recorded_at >= :startAt
                             AND movement.recorded_at < :endAt
                       ), 0) AS cash_out_gross,
                       COALESCE((
                           SELECT SUM(movement.amount)
                           FROM cash_register_movements movement
                           JOIN cash_register_sessions movement_session
                             ON movement_session.id = movement.session_id
                           WHERE movement_session.branch_id = :branchId
                             AND movement.movement_type = 'CASH_OUT'
                             AND movement.status = 'VOIDED'
                             AND movement.voided_at >= :startAt
                             AND movement.voided_at < :endAt
                       ), 0) AS cash_out_voided
                FROM cash_register_sessions session
                WHERE session.branch_id = :branchId
                  AND session.opened_at < :endAt
                """,
                parameters(branchId, start, end),
                (result, rowNumber) -> new CashMetricsRow(
                        result.getLong("opened_session_count"),
                        result.getLong("closed_session_count"),
                        result.getLong("open_at_end_count"),
                        result.getBigDecimal("opening_amount"),
                        result.getBigDecimal("closing_difference"),
                        result.getBigDecimal("cash_in_gross"),
                        result.getBigDecimal("cash_in_voided"),
                        result.getBigDecimal("cash_out_gross"),
                        result.getBigDecimal("cash_out_voided")
                )
        );
    }

    List<DailyReportResponse.CashSessionSummary> findCashSessions(
            Long branchId,
            Instant start,
            Instant end
    ) {
        return jdbc.query(
                """
                SELECT session.id,
                       session.status,
                       session.opened_at,
                       session.closed_at,
                       session.opening_amount,
                       session.expected_cash_at_close,
                       session.counted_cash_at_close,
                       session.difference_at_close,
                       opened_by.full_name AS opened_by_name,
                       closed_by.full_name AS closed_by_name
                FROM cash_register_sessions session
                JOIN app_users opened_by ON opened_by.id = session.opened_by_user_id
                LEFT JOIN app_users closed_by ON closed_by.id = session.closed_by_user_id
                WHERE session.branch_id = :branchId
                  AND session.opened_at < :endAt
                  AND (session.closed_at IS NULL OR session.closed_at >= :startAt)
                ORDER BY session.opened_at
                """,
                parameters(branchId, start, end),
                (result, rowNumber) -> new DailyReportResponse.CashSessionSummary(
                        result.getLong("id"),
                        "CAJ-%06d".formatted(result.getLong("id")),
                        result.getString("status"),
                        result.getString("opened_by_name"),
                        instant(result.getTimestamp("opened_at")),
                        result.getString("closed_by_name"),
                        instant(result.getTimestamp("closed_at")),
                        result.getBigDecimal("opening_amount"),
                        result.getBigDecimal("expected_cash_at_close"),
                        result.getBigDecimal("counted_cash_at_close"),
                        result.getBigDecimal("difference_at_close")
                )
        );
    }

    List<DailyReportResponse.CancelledOrderSummary> findCancelledOrders(
            Long branchId,
            Instant start,
            Instant end
    ) {
        return jdbc.query(
                """
                WITH modifier_totals AS (
                    SELECT modifier.order_item_id,
                           COALESCE(SUM(modifier.price_delta), 0) AS modifier_total
                    FROM order_item_modifiers modifier
                    GROUP BY modifier.order_item_id
                ),
                order_totals AS (
                    SELECT item.order_id,
                           COALESCE(SUM(item.quantity * (
                               item.unit_price + COALESCE(modifier.modifier_total, 0)
                           )), 0) AS amount
                    FROM order_items item
                    LEFT JOIN modifier_totals modifier ON modifier.order_item_id = item.id
                    GROUP BY item.order_id
                )
                SELECT orders.id,
                       orders.service_mode,
                       orders.customer_reference,
                       orders.closed_at,
                       assigned.full_name AS assigned_user_name,
                       COALESCE(total.amount, 0) AS amount
                FROM restaurant_orders orders
                JOIN app_users assigned ON assigned.id = orders.assigned_user_id
                LEFT JOIN order_totals total ON total.order_id = orders.id
                WHERE orders.branch_id = :branchId
                  AND orders.status = 'CANCELLED'
                  AND orders.closed_at >= :startAt
                  AND orders.closed_at < :endAt
                ORDER BY orders.closed_at DESC
                """,
                parameters(branchId, start, end),
                (result, rowNumber) -> new DailyReportResponse.CancelledOrderSummary(
                        result.getLong("id"),
                        "PED-%06d".formatted(result.getLong("id")),
                        result.getString("service_mode"),
                        result.getString("customer_reference"),
                        result.getString("assigned_user_name"),
                        instant(result.getTimestamp("closed_at")),
                        result.getBigDecimal("amount")
                )
        );
    }

    List<DailyReportResponse.VoidedPaymentSummary> findVoidedPayments(
            Long branchId,
            Instant start,
            Instant end
    ) {
        return jdbc.query(
                """
                SELECT payment.id,
                       payment.order_id,
                       payment.method,
                       payment.amount,
                       payment.voided_at,
                       payment.void_reason,
                       voided_by.full_name AS voided_by_name
                FROM order_payments payment
                JOIN restaurant_orders orders ON orders.id = payment.order_id
                JOIN app_users voided_by ON voided_by.id = payment.voided_by_user_id
                WHERE orders.branch_id = :branchId
                  AND payment.status = 'VOIDED'
                  AND payment.voided_at >= :startAt
                  AND payment.voided_at < :endAt
                ORDER BY payment.voided_at DESC
                """,
                parameters(branchId, start, end),
                (result, rowNumber) -> new DailyReportResponse.VoidedPaymentSummary(
                        result.getLong("id"),
                        "PAG-%06d".formatted(result.getLong("id")),
                        result.getLong("order_id"),
                        "PED-%06d".formatted(result.getLong("order_id")),
                        PaymentMethod.valueOf(result.getString("method")),
                        result.getBigDecimal("amount"),
                        instant(result.getTimestamp("voided_at")),
                        result.getString("voided_by_name"),
                        result.getString("void_reason")
                )
        );
    }

    private MapSqlParameterSource parameters(Long branchId, Instant start, Instant end) {
        return new MapSqlParameterSource()
                .addValue("branchId", branchId)
                .addValue("startAt", OffsetDateTime.ofInstant(start, ZoneOffset.UTC))
                .addValue("endAt", OffsetDateTime.ofInstant(end, ZoneOffset.UTC));
    }

    private Instant instant(java.sql.Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant();
    }

    record PaymentMethodRow(
            PaymentMethod method,
            BigDecimal grossAmount,
            BigDecimal voidedAmount,
            long receivedCount,
            long voidedCount
    ) {
    }

    record OrderMetricsRow(
            long openedCount,
            long completedCount,
            long cancelledCount,
            long pendingAtEndCount,
            long dineInCount,
            long takeoutCount,
            long guestCount,
            BigDecimal completedOrderValue
    ) {
    }

    record ProductSalesRow(String productName, long quantity, BigDecimal amount) {
    }

    record CashMetricsRow(
            long openedSessionCount,
            long closedSessionCount,
            long openAtEndCount,
            BigDecimal openingAmount,
            BigDecimal closingDifference,
            BigDecimal cashInGross,
            BigDecimal cashInVoided,
            BigDecimal cashOutGross,
            BigDecimal cashOutVoided
    ) {
    }
}
