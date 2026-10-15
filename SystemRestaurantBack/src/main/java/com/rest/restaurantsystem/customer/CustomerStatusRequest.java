package com.rest.restaurantsystem.customer;

import jakarta.validation.constraints.NotNull;

public record CustomerStatusRequest(@NotNull Boolean active, @NotNull Long expectedVersion) { }
