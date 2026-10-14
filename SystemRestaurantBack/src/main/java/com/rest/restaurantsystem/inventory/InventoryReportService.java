package com.rest.restaurantsystem.inventory;

import com.rest.restaurantsystem.exception.BadRequestException;
import com.rest.restaurantsystem.structure.BranchResponse;
import com.rest.restaurantsystem.structure.BranchService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

@Service
public class InventoryReportService {
    private final IngredientRepository ingredients;
    private final InventoryReportQueryRepository query;
    private final BranchService branches;

    public InventoryReportService(IngredientRepository ingredients,
                                  InventoryReportQueryRepository query, BranchService branches) {
        this.ingredients = ingredients;
        this.query = query;
        this.branches = branches;
    }

    @Transactional(readOnly = true)
    public InventoryReportResponse report(LocalDate from, LocalDate to) {
        if (from.isAfter(to) || ChronoUnit.DAYS.between(from, to) > 30) {
            throw new BadRequestException("Selecciona un periodo de hasta 31 días.");
        }
        // V10 guarantees one local restaurant. Its configured timezone defines the day boundaries.
        BranchResponse branch = branches.findAll().stream().findFirst()
                .orElseThrow(() -> new BadRequestException("Configura primero el restaurante."));
        ZoneId zone = ZoneId.of(branch.timezone());
        if (to.isAfter(LocalDate.now(zone))) {
            throw new BadRequestException("No se puede consultar una fecha futura.");
        }
        Instant start = from.atStartOfDay(zone).toInstant();
        Instant end = to.plusDays(1).atStartOfDay(zone).toInstant();
        Map<Long, InventoryReportQueryRepository.Totals> byIngredient = query.totals(start, end);
        List<InventoryReportResponse.IngredientSummary> rows = ingredients.findAllByOrderByNameAsc()
                .stream().map(ingredient -> {
                    InventoryReportQueryRepository.Totals totals = byIngredient.getOrDefault(
                            ingredient.getId(), new InventoryReportQueryRepository.Totals());
                    BigDecimal cost = ingredient.getUnitCost();
                    boolean active = ingredient.isActive();
                    BigDecimal stock = ingredient.getStockQuantity();
                    return new InventoryReportResponse.IngredientSummary(
                            ingredient.getId(), ingredient.getName(), ingredient.getUnit(), active,
                            stock, ingredient.getMinimumQuantity(), cost,
                            cost == null ? null : stock.multiply(cost),
                            active && ingredient.getMinimumQuantity().signum() > 0
                                    && stock.compareTo(ingredient.getMinimumQuantity()) <= 0,
                            active && stock.signum() == 0,
                            totals.purchases, totals.manualUse, totals.waste, totals.adjustments,
                            totals.consumption, totals.reversal, totals.net,
                            totals.knownOrderCost, totals.unpricedOrderMovements);
                }).toList();
        return new InventoryReportResponse(from, to, zone.getId(), Instant.now(),
                (int) rows.stream().filter(InventoryReportResponse.IngredientSummary::active).count(),
                (int) rows.stream().filter(InventoryReportResponse.IngredientSummary::lowStock).count(),
                (int) rows.stream().filter(InventoryReportResponse.IngredientSummary::outOfStock).count(),
                (int) rows.stream().filter(row -> row.active() && row.unitCost() == null).count(),
                (int) rows.stream().filter(row -> row.active() && row.stockQuantity().signum() > 0
                        && row.unitCost() == null).count(),
                rows.stream().filter(InventoryReportResponse.IngredientSummary::active)
                        .map(InventoryReportResponse.IngredientSummary::stockValue)
                        .filter(value -> value != null).reduce(BigDecimal.ZERO, BigDecimal::add),
                rows.stream().map(InventoryReportResponse.IngredientSummary::knownOrderCost)
                        .reduce(BigDecimal.ZERO, BigDecimal::add),
                rows.stream().mapToLong(InventoryReportResponse.IngredientSummary::unpricedOrderMovements).sum(),
                rows);
    }
}
