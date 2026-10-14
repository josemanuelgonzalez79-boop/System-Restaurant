package com.rest.restaurantsystem.inventory;

import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/recipes")
public class RecipeController {

    private final RecipeService service;

    public RecipeController(RecipeService service) {
        this.service = service;
    }

    @GetMapping("/products/{productId}")
    public RecipeResponse findProduct(@PathVariable Long productId) {
        return service.find(RecipeOwnerType.PRODUCT, productId);
    }

    @PutMapping("/products/{productId}")
    public RecipeResponse saveProduct(
            @PathVariable Long productId, @Valid @RequestBody RecipeSaveRequest request,
            Authentication authentication
    ) {
        return service.save(RecipeOwnerType.PRODUCT, productId, request, authentication.getName());
    }

    @GetMapping("/modifier-options/{optionId}")
    public RecipeResponse findOption(@PathVariable Long optionId) {
        return service.find(RecipeOwnerType.MODIFIER_OPTION, optionId);
    }

    @PutMapping("/modifier-options/{optionId}")
    public RecipeResponse saveOption(
            @PathVariable Long optionId, @Valid @RequestBody RecipeSaveRequest request,
            Authentication authentication
    ) {
        return service.save(RecipeOwnerType.MODIFIER_OPTION, optionId, request, authentication.getName());
    }
}
