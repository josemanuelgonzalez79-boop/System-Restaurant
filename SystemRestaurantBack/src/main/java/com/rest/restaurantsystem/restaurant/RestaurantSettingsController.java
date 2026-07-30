package com.rest.restaurantsystem.restaurant;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/settings")
public class RestaurantSettingsController {

    private final RestaurantSettingsService service;

    public RestaurantSettingsController(RestaurantSettingsService service) {
        this.service = service;
    }

    @GetMapping
    public RestaurantSettingsResponse get() {
        return service.get();
    }

    @PutMapping
    public RestaurantSettingsResponse update(@Valid @RequestBody RestaurantSettingsRequest request) {
        return service.update(request);
    }
}
