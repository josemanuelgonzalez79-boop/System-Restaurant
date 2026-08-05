package com.rest.restaurantsystem.business;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/settings")
public class BusinessSettingsController {

    private final BusinessSettingsService service;

    public BusinessSettingsController(BusinessSettingsService service) {
        this.service = service;
    }

    @GetMapping
    public BusinessSettingsResponse get() {
        return service.get();
    }

    @PutMapping
    public BusinessSettingsResponse update(@Valid @RequestBody BusinessSettingsRequest request) {
        return service.update(request);
    }
}
