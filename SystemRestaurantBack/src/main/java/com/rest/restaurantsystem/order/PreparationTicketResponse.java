package com.rest.restaurantsystem.order;

import com.rest.restaurantsystem.catalog.product.ProductDestination;

import java.time.Instant;
import java.util.List;

public record PreparationTicketResponse(
        Long id,
        OrderResponse order,
        ProductDestination destination,
        int sequenceNumber,
        Long sentByUserId,
        String sentByUserName,
        Instant sentAt,
        PreparationStatus status,
        List<PreparationItemResponse> items
) {
}
