package com.rest.restaurantsystem.catalog.modifier;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record ModifierGroupRequest(
        @NotNull @Positive Long productId,
        @NotBlank @Size(max = 100) String name,
        @Min(0) @Max(20) int minSelections,
        @Min(1) @Max(20) int maxSelections,
        @Min(0) int sortOrder
) {
}
