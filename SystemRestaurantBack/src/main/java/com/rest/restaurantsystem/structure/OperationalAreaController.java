package com.rest.restaurantsystem.structure;

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
@RequestMapping("/api/v1/areas")
public class OperationalAreaController {

    private final OperationalAreaService service;

    public OperationalAreaController(OperationalAreaService service) {
        this.service = service;
    }

    @GetMapping
    public List<OperationalAreaResponse> findAll(
            @RequestParam(required = false) Long branchId
    ) {
        return service.findAll(branchId);
    }

    @GetMapping("/{id}")
    public OperationalAreaResponse findById(@PathVariable Long id) {
        return service.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OperationalAreaResponse create(@Valid @RequestBody OperationalAreaRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    public OperationalAreaResponse update(
            @PathVariable Long id,
            @Valid @RequestBody OperationalAreaRequest request
    ) {
        return service.update(id, request);
    }

    @PatchMapping("/{id}/active")
    public OperationalAreaResponse changeActive(
            @PathVariable Long id,
            @RequestBody ActiveStatusRequest request
    ) {
        return service.changeActive(id, request.active());
    }
}
