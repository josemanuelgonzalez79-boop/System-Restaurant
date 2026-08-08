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
@RequestMapping("/api/v1/service-points")
public class ServicePointController {

    private final ServicePointService service;

    public ServicePointController(ServicePointService service) {
        this.service = service;
    }

    @GetMapping
    public List<ServicePointResponse> findAll(@RequestParam(required = false) Long areaId) {
        return service.findAll(areaId);
    }

    @GetMapping("/{id}")
    public ServicePointResponse findById(@PathVariable Long id) {
        return service.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ServicePointResponse create(@Valid @RequestBody ServicePointRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    public ServicePointResponse update(
            @PathVariable Long id,
            @Valid @RequestBody ServicePointRequest request
    ) {
        return service.update(id, request);
    }

    @PatchMapping("/{id}/active")
    public ServicePointResponse changeActive(
            @PathVariable Long id,
            @RequestBody ActiveStatusRequest request
    ) {
        return service.changeActive(id, request.active());
    }
}
