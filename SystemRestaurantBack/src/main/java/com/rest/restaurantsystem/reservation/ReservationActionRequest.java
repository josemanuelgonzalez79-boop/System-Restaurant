package com.rest.restaurantsystem.reservation;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ReservationActionRequest(
        @NotNull @Min(0) Long expectedVersion,
        @Size(max = 250) String reason
) { }
