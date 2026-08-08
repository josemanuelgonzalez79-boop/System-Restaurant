package com.rest.restaurantsystem.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record PasswordResetRequest(
        @NotBlank(message = PasswordPolicy.REQUIRED_MESSAGE)
        @Size(
                min = PasswordPolicy.MIN_LENGTH,
                max = PasswordPolicy.MAX_LENGTH,
                message = PasswordPolicy.LENGTH_MESSAGE
        )
        @Pattern(
                regexp = PasswordPolicy.COMPOSITION_REGEX,
                message = PasswordPolicy.COMPOSITION_MESSAGE
        )
        String password
) {
}
