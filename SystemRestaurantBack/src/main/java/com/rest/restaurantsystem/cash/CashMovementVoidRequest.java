package com.rest.restaurantsystem.cash;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CashMovementVoidRequest(
        long sessionVersion,
        @NotBlank @Size(max = 250) String reason
) {
}
