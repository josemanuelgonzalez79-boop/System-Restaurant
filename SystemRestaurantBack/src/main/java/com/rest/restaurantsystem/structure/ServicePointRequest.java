package com.rest.restaurantsystem.structure;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record ServicePointRequest(
        @NotNull @Positive Long areaId,
        @NotBlank
        @Size(max = 40)
        @Pattern(regexp = "^[A-Za-z0-9_-]+$", message = "solo puede contener letras, números, guion y guion bajo")
        String code,
        @NotBlank @Size(max = 120) String name,
        @Size(max = 250) String description,
        @NotNull ServicePointType pointType,
        @NotNull @Min(0) Integer sortOrder
) {
}
