package com.rest.restaurantsystem.order;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/orders/{orderId}/discounts")
public class OrderDiscountController {
    private final OrderDiscountService service;

    public OrderDiscountController(OrderDiscountService service) { this.service = service; }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderPaymentSummaryResponse apply(@PathVariable Long orderId,
                                             @Valid @RequestBody DiscountApplyRequest request,
                                             Authentication authentication) {
        return service.apply(orderId, request, authentication.getName());
    }

    @PatchMapping("/{discountId}/void")
    public OrderPaymentSummaryResponse voidDiscount(@PathVariable Long orderId,
                                                    @PathVariable Long discountId,
                                                    @Valid @RequestBody DiscountVoidRequest request,
                                                    Authentication authentication) {
        return service.voidDiscount(orderId, discountId, request, authentication.getName());
    }
}
