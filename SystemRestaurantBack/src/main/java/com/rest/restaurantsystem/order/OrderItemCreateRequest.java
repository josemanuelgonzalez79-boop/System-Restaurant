package com.rest.restaurantsystem.order;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.util.List;

public record OrderItemCreateRequest(
        @NotNull @Positive Long productId,
        @Min(1) @Max(99) int quantity,
        @Size(max = 500) String notes,
        @NotNull @Size(max = 50) List<@Positive Long> modifierOptionIds,
        @PositiveOrZero long orderVersion
) {
}
