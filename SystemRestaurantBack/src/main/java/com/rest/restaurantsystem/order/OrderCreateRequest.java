package com.rest.restaurantsystem.order;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record OrderCreateRequest(
        @NotNull Long branchId,
        Long servicePointId,
        @NotNull ServiceMode serviceMode,
        @NotNull Long assignedUserId,
        @NotNull @Min(1) @Max(99) Integer guestCount,
        @Size(max = 120) String customerReference,
        @Size(max = 500) String notes,
        Long reservationId
) {
    public OrderCreateRequest(Long branchId, Long servicePointId, ServiceMode serviceMode,
                              Long assignedUserId, Integer guestCount, String customerReference,
                              String notes) {
        this(branchId, servicePointId, serviceMode, assignedUserId, guestCount,
                customerReference, notes, null);
    }
}
