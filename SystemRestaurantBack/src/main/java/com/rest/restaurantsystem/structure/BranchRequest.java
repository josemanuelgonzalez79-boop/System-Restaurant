package com.rest.restaurantsystem.structure;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record BranchRequest(
        @NotBlank
        @Size(max = 30)
        @Pattern(regexp = "^[A-Za-z0-9_-]+$", message = "solo puede contener letras, números, guion y guion bajo")
        String code,
        @NotBlank @Size(max = 120) String name,
        @Size(max = 250) String address,
        @Size(max = 30) String phone,
        @NotBlank @Size(max = 60) String timezone,
        @NotNull @Min(0) Integer sortOrder
) {
}
