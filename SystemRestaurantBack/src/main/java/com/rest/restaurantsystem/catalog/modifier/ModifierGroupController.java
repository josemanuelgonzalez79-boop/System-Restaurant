package com.rest.restaurantsystem.catalog.modifier;

import com.rest.restaurantsystem.common.ActiveStatusRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/modifier-groups")
public class ModifierGroupController {

    private final ModifierGroupService service;

    public ModifierGroupController(ModifierGroupService service) {
        this.service = service;
    }

    @GetMapping
    public List<ModifierGroupResponse> findAll(
            @RequestParam(required = false) Long productId
    ) {
        return service.findAll(productId);
    }

    @GetMapping("/{id}")
    public ModifierGroupResponse findById(@PathVariable Long id) {
        return service.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ModifierGroupResponse create(@Valid @RequestBody ModifierGroupRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    public ModifierGroupResponse update(
            @PathVariable Long id,
            @Valid @RequestBody ModifierGroupRequest request
    ) {
        return service.update(id, request);
    }

    @PatchMapping("/{id}/active")
    public ModifierGroupResponse changeActive(
            @PathVariable Long id,
            @RequestBody ActiveStatusRequest request
    ) {
        return service.changeActive(id, request.active());
    }
}
