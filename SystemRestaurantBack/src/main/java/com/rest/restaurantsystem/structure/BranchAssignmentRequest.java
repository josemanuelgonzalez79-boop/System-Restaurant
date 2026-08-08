package com.rest.restaurantsystem.structure;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

public record BranchAssignmentRequest(
        @NotNull @Size(min = 1) List<@Valid @NotNull @Positive Long> userIds
) {
}
