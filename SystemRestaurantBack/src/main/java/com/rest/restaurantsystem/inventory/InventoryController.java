package com.rest.restaurantsystem.inventory;

import com.rest.restaurantsystem.common.ActiveStatusRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/inventory/ingredients")
public class InventoryController {

    private final InventoryService service;

    public InventoryController(InventoryService service) {
        this.service = service;
    }

    @GetMapping
    public List<IngredientResponse> findAll() {
        return service.findAll();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public IngredientResponse create(@Valid @RequestBody IngredientRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    public IngredientResponse update(@PathVariable Long id, @Valid @RequestBody IngredientRequest request) {
        return service.update(id, request);
    }

    @PatchMapping("/{id}/active")
    public IngredientResponse changeActive(@PathVariable Long id, @RequestBody ActiveStatusRequest request) {
        return service.changeActive(id, request.active());
    }

    @GetMapping("/{id}/movements")
    public List<InventoryMovementResponse> findMovements(@PathVariable Long id) {
        return service.findMovements(id);
    }

    @PostMapping("/{id}/movements")
    @ResponseStatus(HttpStatus.CREATED)
    public InventoryMovementResponse recordMovement(
            @PathVariable Long id,
            @Valid @RequestBody StockMovementRequest request,
            Authentication authentication
    ) {
        return service.recordMovement(id, request, authentication.getName());
    }
}
