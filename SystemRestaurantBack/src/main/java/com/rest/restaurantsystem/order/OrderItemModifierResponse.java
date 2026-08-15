package com.rest.restaurantsystem.order;

import java.math.BigDecimal;

public record OrderItemModifierResponse(
        Long id,
        Long modifierOptionId,
        String groupName,
        String optionName,
        BigDecimal priceDelta
) {

    static OrderItemModifierResponse from(OrderItemModifier modifier) {
        return new OrderItemModifierResponse(
                modifier.getId(),
                modifier.getModifierOptionId(),
                modifier.getGroupName(),
                modifier.getOptionName(),
                modifier.getPriceDelta()
        );
    }
}
