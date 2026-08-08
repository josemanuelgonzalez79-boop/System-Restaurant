package com.rest.restaurantsystem.order;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record OrderStatusRequest(
        @NotNull OrderStatus status,
        @PositiveOrZero long version
) {
}
