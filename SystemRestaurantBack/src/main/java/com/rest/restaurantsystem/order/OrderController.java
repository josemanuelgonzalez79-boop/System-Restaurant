package com.rest.restaurantsystem.order;

import com.rest.restaurantsystem.structure.BranchResponse;
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
@RequestMapping("/api/v1/orders")
public class OrderController {

    private final OrderService service;

    public OrderController(OrderService service) {
        this.service = service;
    }

    @GetMapping
    public List<OrderResponse> findAll(
            @RequestParam(required = false) Long branchId,
            @RequestParam(defaultValue = "true") boolean activeOnly,
            Authentication authentication
    ) {
        return service.findAll(branchId, activeOnly, authentication.getName());
    }

    @GetMapping("/{id}")
    public OrderResponse findById(@PathVariable Long id, Authentication authentication) {
        return service.findById(id, authentication.getName());
    }

    @GetMapping("/branches")
    public List<BranchResponse> findBranches(Authentication authentication) {
        return service.findBranches(authentication.getName());
    }

    @GetMapping("/operators")
    public List<OrderOperatorResponse> findOperators(
            @RequestParam Long branchId,
            Authentication authentication
    ) {
        return service.findOperators(branchId, authentication.getName());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse create(
            @Valid @RequestBody OrderCreateRequest request,
            Authentication authentication
    ) {
        return service.create(request, authentication.getName());
    }

    @PatchMapping("/{id}/status")
    public OrderResponse changeStatus(
            @PathVariable Long id,
            @Valid @RequestBody OrderStatusRequest request,
            Authentication authentication
    ) {
        return service.changeStatus(id, request, authentication.getName());
    }
}
