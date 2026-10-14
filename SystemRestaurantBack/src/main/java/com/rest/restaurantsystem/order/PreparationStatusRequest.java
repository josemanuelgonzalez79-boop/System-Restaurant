package com.rest.restaurantsystem.order;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record PreparationStatusRequest(
        @NotNull PreparationStatus status,
        @PositiveOrZero long version,
        @Size(max = 180) String reason
) {
    public PreparationStatusRequest(PreparationStatus status, long version) {
        this(status, version, null);
    }
}
