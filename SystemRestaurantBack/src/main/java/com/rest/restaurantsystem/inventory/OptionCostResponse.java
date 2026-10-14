package com.rest.restaurantsystem.inventory;

import java.math.BigDecimal;

public record OptionCostResponse(
        Long id, String groupName, String name, BigDecimal priceDelta, boolean active,
        RecipeCostResponse recipe, BigDecimal grossProfit, BigDecimal marginPercent
) {
}
