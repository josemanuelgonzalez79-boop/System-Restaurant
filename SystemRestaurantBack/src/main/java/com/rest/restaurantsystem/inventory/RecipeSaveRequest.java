package com.rest.restaurantsystem.inventory;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record RecipeSaveRequest(
        Long expectedVersion,
        @NotNull @Size(max = 50) List<@Valid RecipeLineRequest> lines
) {
}
