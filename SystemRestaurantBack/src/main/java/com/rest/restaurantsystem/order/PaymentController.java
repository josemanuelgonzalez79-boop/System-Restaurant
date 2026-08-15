package com.rest.restaurantsystem.order;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/orders/{orderId}/payments")
public class PaymentController {

    private final PaymentService service;

    public PaymentController(PaymentService service) {
        this.service = service;
    }

    @GetMapping
    public OrderPaymentSummaryResponse findSummary(
            @PathVariable Long orderId,
            Authentication authentication
    ) {
        return service.findSummary(orderId, authentication.getName());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderPaymentSummaryResponse add(
            @PathVariable Long orderId,
            @Valid @RequestBody PaymentCreateRequest request,
            Authentication authentication
    ) {
        return service.add(orderId, request, authentication.getName());
    }

    @PatchMapping("/{paymentId}/void")
    public OrderPaymentSummaryResponse voidPayment(
            @PathVariable Long orderId,
            @PathVariable Long paymentId,
            @Valid @RequestBody PaymentVoidRequest request,
            Authentication authentication
    ) {
        return service.voidPayment(orderId, paymentId, request, authentication.getName());
    }

    @PostMapping("/close")
    public OrderPaymentSummaryResponse close(
            @PathVariable Long orderId,
            @RequestBody PaymentCloseRequest request,
            Authentication authentication
    ) {
        return service.close(orderId, request, authentication.getName());
    }
}
