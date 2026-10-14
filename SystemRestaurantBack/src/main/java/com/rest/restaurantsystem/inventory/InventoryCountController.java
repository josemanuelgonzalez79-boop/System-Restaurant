package com.rest.restaurantsystem.inventory;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/inventory/counts")
public class InventoryCountController {
    private final InventoryCountService service;

    public InventoryCountController(InventoryCountService service) { this.service = service; }

    @GetMapping
    public List<InventoryCountResponse> recent() { return service.recent(); }

    @GetMapping("/{id}")
    public InventoryCountResponse find(@PathVariable Long id) { return service.find(id); }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public InventoryCountResponse reconcile(@Valid @RequestBody InventoryCountRequest request,
                                            Authentication authentication) {
        return service.reconcile(request, authentication.getName());
    }
}
