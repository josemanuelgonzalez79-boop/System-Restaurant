package com.rest.restaurantsystem.business;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record BusinessSettingsRequest(
        @NotBlank @Size(max = 120) String businessName,
        @NotBlank @Size(max = 80) String displayName,
        @NotNull BusinessType businessType,
        @NotBlank
        @Pattern(regexp = "^[A-Za-z]{3}$", message = "debe tener tres letras")
        String currencyCode,
        @NotBlank @Size(max = 60) String timezone,
        @NotBlank
        @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "debe ser un color hexadecimal como #2563EB")
        String primaryColor,
        @NotBlank
        @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "debe ser un color hexadecimal como #111827")
        String secondaryColor,
        @Size(max = 30) String phone,
        @Size(max = 250) String address
) {
}
