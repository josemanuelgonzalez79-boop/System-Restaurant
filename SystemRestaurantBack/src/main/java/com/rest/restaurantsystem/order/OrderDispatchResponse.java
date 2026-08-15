package com.rest.restaurantsystem.order;

import java.util.List;

public record OrderDispatchResponse(
        OrderDetailResponse orderDetail,
        List<PreparationTicketResponse> tickets
) {
}
