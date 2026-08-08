package com.rest.restaurantsystem.order;

import java.time.Instant;

public record OrderResponse(
        Long id,
        String folio,
        Long branchId,
        String branchName,
        Long servicePointId,
        String servicePointName,
        Long areaId,
        String areaName,
        ServiceMode serviceMode,
        Long assignedUserId,
        String assignedUserName,
        Long openedByUserId,
        String openedByUserName,
        int guestCount,
        String customerReference,
        String notes,
        OrderStatus status,
        long version,
        Instant openedAt,
        Instant updatedAt,
        Instant closedAt
) {
}
