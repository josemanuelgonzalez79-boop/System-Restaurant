package com.rest.restaurantsystem.inventory;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record InventoryCountResponse(
        Long id, UUID operationId, String reason, String recordedByUsername,
        Instant countedAt, int adjustedLines, BigDecimal knownDifferenceValue,
        int unpricedDifferences, List<Line> lines
) {
    public record Line(
            Long ingredientId, String ingredientName, String unit, long expectedVersion,
            BigDecimal expectedQuantity, BigDecimal countedQuantity, BigDecimal difference,
            BigDecimal unitCostSnapshot, BigDecimal differenceValue, Long movementId
    ) {
        static Line from(InventoryCountLine line) {
            BigDecimal difference = line.getCountedQuantity().subtract(line.getExpectedQuantity());
            return new Line(line.getIngredientId(), line.getIngredientName(), line.getUnit(),
                    line.getExpectedVersion(), line.getExpectedQuantity(), line.getCountedQuantity(),
                    difference, line.getUnitCostSnapshot(), line.getUnitCostSnapshot() == null ? null
                    : difference.multiply(line.getUnitCostSnapshot()), line.getMovementId());
        }
    }

    static InventoryCountResponse from(InventoryCount count, List<InventoryCountLine> lines) {
        List<Line> items = lines.stream().map(Line::from).toList();
        return new InventoryCountResponse(count.getId(), count.getOperationId(), count.getReason(),
                count.getRecordedByUsername(), count.getCountedAt(),
                (int) items.stream().filter(item -> item.difference().signum() != 0).count(),
                items.stream().map(Line::differenceValue).filter(value -> value != null)
                        .reduce(BigDecimal.ZERO, BigDecimal::add),
                (int) items.stream().filter(item -> item.difference().signum() != 0
                        && item.differenceValue() == null).count(), items);
    }
}
