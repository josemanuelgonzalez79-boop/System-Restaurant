package com.rest.restaurantsystem.order;

import java.math.BigDecimal;
import java.util.List;

public record OrderDetailResponse(
        OrderResponse order,
        List<OrderItemResponse> items,
        int totalUnits,
        BigDecimal productSubtotal,
        BigDecimal modifierSubtotal,
        BigDecimal total
) {
}
