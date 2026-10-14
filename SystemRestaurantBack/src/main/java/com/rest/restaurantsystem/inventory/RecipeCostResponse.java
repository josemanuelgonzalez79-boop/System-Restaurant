package com.rest.restaurantsystem.inventory;

import java.math.BigDecimal;
import java.util.List;

public record RecipeCostResponse(
        boolean configured, BigDecimal cost, List<String> missingCosts,
        List<CostLineResponse> lines
) {
}
