package com.rest.restaurantsystem.order;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DiscountVoidRequest(long orderVersion, @NotBlank @Size(max = 250) String reason) { }
