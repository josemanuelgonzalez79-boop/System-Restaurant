package com.rest.restaurantsystem.order;

import jakarta.validation.constraints.PositiveOrZero;

public record PreparationDispatchRequest(
        @PositiveOrZero long orderVersion
) {
}
