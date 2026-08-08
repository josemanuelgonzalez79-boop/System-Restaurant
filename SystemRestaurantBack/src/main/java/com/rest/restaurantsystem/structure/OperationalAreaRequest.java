package com.rest.restaurantsystem.structure;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record OperationalAreaRequest(
        @NotNull @Positive Long branchId,
        @NotBlank @Size(max = 120) String name,
        @Size(max = 250) String description,
        @NotNull AreaType areaType,
        @NotNull @Min(0) Integer sortOrder
) {
}
