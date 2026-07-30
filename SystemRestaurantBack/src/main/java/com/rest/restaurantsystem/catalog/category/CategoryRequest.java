package com.rest.restaurantsystem.catalog.category;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CategoryRequest(
        @NotBlank @Size(max = 80) String name,
        @Size(max = 250) String description,
        @NotNull @Min(0) Integer sortOrder
) {
}
