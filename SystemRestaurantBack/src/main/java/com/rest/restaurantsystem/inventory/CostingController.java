package com.rest.restaurantsystem.inventory;

import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/costs")
public class CostingController {
    private final CostingService service;

    public CostingController(CostingService service) {
        this.service = service;
    }

    @GetMapping("/catalog")
    public List<ProductCostResponse> catalog() {
        return service.catalog();
    }

    @GetMapping("/ingredients/{ingredientId}/history")
    public List<CostChangeResponse> history(@PathVariable Long ingredientId) {
        return service.history(ingredientId);
    }

    @PutMapping("/ingredients/{ingredientId}")
    public IngredientResponse updateCost(
            @PathVariable Long ingredientId,
            @Valid @RequestBody CostUpdateRequest request,
            Authentication authentication
    ) {
        return service.updateCost(ingredientId, request, authentication.getName());
    }
}
