package com.rest.restaurantsystem.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UserCreateRequest(
        @NotBlank
        @Size(min = 4, max = 80)
        @Pattern(
                regexp = "^[A-Za-z0-9._-]+$",
                message = "solo puede contener letras, números, punto, guion y guion bajo"
        )
        String username,
        @NotBlank @Size(min = 2, max = 120) String fullName,
        @NotNull UserRole role,
        @NotBlank @Size(min = 10, max = 72) String password
) {
}
