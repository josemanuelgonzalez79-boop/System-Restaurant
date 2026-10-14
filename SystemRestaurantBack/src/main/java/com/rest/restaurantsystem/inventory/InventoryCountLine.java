package com.rest.restaurantsystem.inventory;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(name = "inventory_count_lines")
class InventoryCountLine {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "count_id", nullable = false, updatable = false)
    private Long countId;
    @Column(name = "ingredient_id", nullable = false, updatable = false)
    private Long ingredientId;
    @Column(name = "ingredient_name", nullable = false, length = 120, updatable = false)
    private String ingredientName;
    @Column(nullable = false, length = 10, updatable = false)
    private String unit;
    @Column(name = "expected_version", nullable = false, updatable = false)
    private long expectedVersion;
    @Column(name = "expected_quantity", nullable = false, precision = 14, scale = 3, updatable = false)
    private BigDecimal expectedQuantity;
    @Column(name = "counted_quantity", nullable = false, precision = 14, scale = 3, updatable = false)
    private BigDecimal countedQuantity;
    @Column(name = "unit_cost_snapshot", precision = 14, scale = 4, updatable = false)
    private BigDecimal unitCostSnapshot;
    @Column(name = "movement_id", updatable = false)
    private Long movementId;

    protected InventoryCountLine() { }

    InventoryCountLine(Long countId, Ingredient ingredient, long expectedVersion,
                       BigDecimal expected, BigDecimal counted, Long movementId) {
        this.countId = countId;
        this.ingredientId = ingredient.getId();
        this.ingredientName = ingredient.getName();
        this.unit = ingredient.getUnit().name();
        this.expectedVersion = expectedVersion;
        this.expectedQuantity = expected;
        this.countedQuantity = counted;
        this.unitCostSnapshot = ingredient.getUnitCost();
        this.movementId = movementId;
    }

    Long getId() { return id; }
    Long getCountId() { return countId; }
    Long getIngredientId() { return ingredientId; }
    String getIngredientName() { return ingredientName; }
    String getUnit() { return unit; }
    long getExpectedVersion() { return expectedVersion; }
    BigDecimal getExpectedQuantity() { return expectedQuantity; }
    BigDecimal getCountedQuantity() { return countedQuantity; }
    BigDecimal getUnitCostSnapshot() { return unitCostSnapshot; }
    Long getMovementId() { return movementId; }
}
