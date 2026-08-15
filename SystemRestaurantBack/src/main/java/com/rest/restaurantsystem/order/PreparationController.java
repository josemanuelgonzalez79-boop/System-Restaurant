package com.rest.restaurantsystem.order;

import com.rest.restaurantsystem.catalog.product.ProductDestination;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class PreparationController {

    private final PreparationService service;

    public PreparationController(PreparationService service) {
        this.service = service;
    }

    @PostMapping("/api/v1/orders/{orderId}/dispatch")
    @ResponseStatus(HttpStatus.CREATED)
    public OrderDispatchResponse dispatch(
            @PathVariable Long orderId,
            @Valid @RequestBody PreparationDispatchRequest request,
            Authentication authentication
    ) {
        return service.dispatch(orderId, request, authentication.getName());
    }

    @GetMapping("/api/v1/preparation")
    public List<PreparationTicketResponse> findAll(
            @RequestParam Long branchId,
            @RequestParam(required = false) ProductDestination destination,
            @RequestParam(defaultValue = "true") boolean activeOnly,
            Authentication authentication
    ) {
        return service.findAll(
                branchId,
                destination,
                activeOnly,
                authentication.getName()
        );
    }

    @PatchMapping("/api/v1/preparation/items/{itemId}/status")
    public PreparationTicketResponse changeStatus(
            @PathVariable Long itemId,
            @Valid @RequestBody PreparationStatusRequest request,
            Authentication authentication
    ) {
        return service.changeStatus(itemId, request, authentication.getName());
    }
}
