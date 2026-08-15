package com.rest.restaurantsystem.order;

import jakarta.validation.Valid;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/orders/{orderId}")
public class OrderItemController {

    private final OrderItemService service;

    public OrderItemController(OrderItemService service) {
        this.service = service;
    }

    @GetMapping("/detail")
    public OrderDetailResponse findDetail(
            @PathVariable Long orderId,
            Authentication authentication
    ) {
        return service.findDetail(orderId, authentication.getName());
    }

    @PostMapping("/items")
    @ResponseStatus(HttpStatus.CREATED)
    public OrderDetailResponse add(
            @PathVariable Long orderId,
            @Valid @RequestBody OrderItemCreateRequest request,
            Authentication authentication
    ) {
        return service.add(orderId, request, authentication.getName());
    }

    @PutMapping("/items/{itemId}")
    public OrderDetailResponse update(
            @PathVariable Long orderId,
            @PathVariable Long itemId,
            @Valid @RequestBody OrderItemUpdateRequest request,
            Authentication authentication
    ) {
        return service.update(orderId, itemId, request, authentication.getName());
    }

    @DeleteMapping("/items/{itemId}")
    public OrderDetailResponse remove(
            @PathVariable Long orderId,
            @PathVariable Long itemId,
            @RequestParam @PositiveOrZero long orderVersion,
            Authentication authentication
    ) {
        return service.remove(orderId, itemId, orderVersion, authentication.getName());
    }
}
