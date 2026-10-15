package com.rest.restaurantsystem.promotion;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
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
@RequestMapping("/api/v1/promotions")
public class PromotionController {
    private final PromotionService service;

    public PromotionController(PromotionService service) { this.service = service; }

    @GetMapping
    public List<PromotionResponse> findAll(
            @RequestParam(defaultValue = "false") boolean availableOnly) {
        return service.findAll(availableOnly);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PromotionResponse create(@Valid @RequestBody PromotionSaveRequest request,
                                    Authentication authentication) {
        return service.create(request, authentication.getName());
    }

    @PutMapping("/{id}")
    public PromotionResponse update(@PathVariable Long id,
                                    @Valid @RequestBody PromotionSaveRequest request,
                                    Authentication authentication) {
        return service.update(id, request, authentication.getName());
    }
}
