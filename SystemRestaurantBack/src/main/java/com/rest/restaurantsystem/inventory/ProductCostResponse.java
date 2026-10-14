package com.rest.restaurantsystem.inventory;

import com.rest.restaurantsystem.catalog.product.ProductDestination;

import java.math.BigDecimal;
import java.util.List;

public record ProductCostResponse(
        Long id, String name, String categoryName, BigDecimal price,
        ProductDestination destination, boolean active, RecipeCostResponse recipe,
        BigDecimal grossProfit, BigDecimal marginPercent, List<OptionCostResponse> options
) {
}
