package com.rest.restaurantsystem.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UserUpdateRequest(
        @NotBlank @Size(min = 2, max = 120) String fullName,
        @NotNull UserRole role
) {
}
