package com.rest.restaurantsystem.order;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record PreparationStatusRequest(
        @NotNull PreparationStatus status,
        @PositiveOrZero long version
) {
}
