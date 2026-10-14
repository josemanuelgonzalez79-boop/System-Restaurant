package com.rest.restaurantsystem.inventory;

import com.rest.restaurantsystem.exception.BadRequestException;
import com.rest.restaurantsystem.structure.BranchResponse;
import com.rest.restaurantsystem.structure.BranchService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InventoryReportServiceTest {
    @Mock IngredientRepository ingredients;
    @Mock InventoryReportQueryRepository query;
    @Mock BranchService branches;
    @InjectMocks InventoryReportService service;

    @Test
    void reportsPartialValuationAndNeverCallsMissingCostZero() {
        Ingredient priced = ingredient(1L, "Harina", "2.000", "40.2500");
        Ingredient unpriced = ingredient(2L, "Leche", "1.000", null);
        when(branches.findAll()).thenReturn(List.of(branch()));
        when(ingredients.findAllByOrderByNameAsc()).thenReturn(List.of(priced, unpriced));
        when(query.totals(any(), any())).thenReturn(Map.of());

        InventoryReportResponse report = service.report(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 1));

        assertEquals(new BigDecimal("80.5000000"), report.knownStockValue());
        assertEquals(1, report.unvaluedStock());
        assertEquals(1, report.missingCosts());
        assertEquals(null, report.ingredients().get(1).stockValue());
    }

    @Test
    void refusesPeriodsLongerThanThirtyOneDays() {
        assertThrows(BadRequestException.class,
                () -> service.report(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 9, 1)));
        verifyNoInteractions(ingredients, query, branches);
    }

    private Ingredient ingredient(Long id, String name, String stock, String cost) {
        Ingredient result = new Ingredient(new IngredientRequest(name, IngredientUnit.KG,
                BigDecimal.ONE, null));
        ReflectionTestUtils.setField(result, "id", id);
        result.setStockQuantity(new BigDecimal(stock));
        if (cost != null) result.setUnitCost(new BigDecimal(cost));
        return result;
    }

    private BranchResponse branch() {
        Instant now = Instant.now();
        return new BranchResponse(1L, "PRINCIPAL", "Principal", null, null,
                "America/Mazatlan", 0, true, 0L, now, now);
    }
}
