package com.rest.restaurantsystem.inventory;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Repository
class InventoryReportQueryRepository {
    private final JdbcTemplate jdbc;

    InventoryReportQueryRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    Map<Long, Totals> totals(Instant start, Instant end) {
        Map<Long, Totals> result = new HashMap<>();
        jdbc.query("""
                SELECT ingredient_id, movement_type, SUM(quantity_delta) AS delta,
                       COALESCE(SUM(cost_amount_snapshot), 0) AS known_cost,
                       COUNT(*) FILTER (WHERE movement_type IN ('ORDER_CONSUMPTION', 'ORDER_REVERSAL')
                           AND cost_amount_snapshot IS NULL) AS unpriced
                FROM inventory_movements
                WHERE occurred_at >= ? AND occurred_at < ?
                GROUP BY ingredient_id, movement_type
                """, row -> {
            long id = row.getLong("ingredient_id");
            Totals totals = result.computeIfAbsent(id, ignored -> new Totals());
            BigDecimal delta = row.getBigDecimal("delta");
            totals.net = totals.net.add(delta);
            switch (row.getString("movement_type")) {
                case "PURCHASE" -> totals.purchases = totals.purchases.add(delta);
                case "USE" -> totals.manualUse = totals.manualUse.add(delta.negate());
                case "WASTE" -> totals.waste = totals.waste.add(delta.negate());
                case "ADJUSTMENT" -> totals.adjustments = totals.adjustments.add(delta);
                case "ORDER_CONSUMPTION" -> totals.consumption = totals.consumption.add(delta.negate());
                case "ORDER_REVERSAL" -> totals.reversal = totals.reversal.add(delta);
                default -> throw new IllegalStateException("Movimiento desconocido");
            }
            totals.knownOrderCost = totals.knownOrderCost.subtract(row.getBigDecimal("known_cost"));
            totals.unpricedOrderMovements += row.getLong("unpriced");
        }, Timestamp.from(start), Timestamp.from(end));
        return result;
    }

    static class Totals {
        BigDecimal purchases = BigDecimal.ZERO;
        BigDecimal manualUse = BigDecimal.ZERO;
        BigDecimal waste = BigDecimal.ZERO;
        BigDecimal adjustments = BigDecimal.ZERO;
        BigDecimal consumption = BigDecimal.ZERO;
        BigDecimal reversal = BigDecimal.ZERO;
        BigDecimal net = BigDecimal.ZERO;
        BigDecimal knownOrderCost = BigDecimal.ZERO;
        long unpricedOrderMovements;
    }
}
