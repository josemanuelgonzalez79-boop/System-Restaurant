package com.rest.restaurantsystem.inventory;

import com.rest.restaurantsystem.catalog.modifier.ModifierOptionService;
import com.rest.restaurantsystem.catalog.product.ProductDestination;
import com.rest.restaurantsystem.catalog.product.ProductResponse;
import com.rest.restaurantsystem.catalog.product.ProductService;
import com.rest.restaurantsystem.exception.BadRequestException;
import com.rest.restaurantsystem.exception.ConflictException;
import com.rest.restaurantsystem.user.UserResponse;
import com.rest.restaurantsystem.user.UserRole;
import com.rest.restaurantsystem.user.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecipeServiceTest {

    @Mock RecipeRepository recipes;
    @Mock RecipeLineRepository lines;
    @Mock IngredientRepository ingredients;
    @Mock ProductService products;
    @Mock ModifierOptionService modifierOptions;
    @Mock UserService users;
    @InjectMocks RecipeService service;

    @Test
    void showsEmptyRecipeForExistingProduct() {
        when(products.findById(7L)).thenReturn(product());

        RecipeResponse result = service.find(RecipeOwnerType.PRODUCT, 7L);

        assertNull(result.id());
        assertEquals("Taco", result.ownerName());
        assertEquals(List.of(), result.lines());
    }

    @Test
    void rejectsDuplicateIngredientInsteadOfSilentlyCountingItTwice() {
        when(products.findById(7L)).thenReturn(product());
        RecipeSaveRequest request = new RecipeSaveRequest(null, List.of(
                new RecipeLineRequest(3L, new BigDecimal("0.100")),
                new RecipeLineRequest(3L, new BigDecimal("0.200"))));

        assertThrows(BadRequestException.class,
                () -> service.save(RecipeOwnerType.PRODUCT, 7L, request, "admin"));
        verifyNoInteractions(ingredients, lines, users);
    }

    @Test
    void rejectsFractionsOfAPiece() {
        when(products.findById(7L)).thenReturn(product());
        when(ingredients.findByIdForUpdate(3L)).thenReturn(Optional.of(ingredient(IngredientUnit.PIECE)));

        assertThrows(BadRequestException.class,
                () -> service.save(RecipeOwnerType.PRODUCT, 7L,
                        new RecipeSaveRequest(null, List.of(
                                new RecipeLineRequest(3L, new BigDecimal("0.5")))), "admin"));
        verifyNoInteractions(lines, users);
    }

    @Test
    void rejectsOlderVersionBeforeReplacingAnyRecipeLines() {
        Recipe recipe = storedRecipe();
        when(products.findById(7L)).thenReturn(product());
        when(recipes.findByProductId(7L)).thenReturn(Optional.of(recipe));
        when(recipes.findByIdForUpdate(12L)).thenReturn(Optional.of(recipe));

        assertThrows(ConflictException.class,
                () -> service.save(RecipeOwnerType.PRODUCT, 7L,
                        new RecipeSaveRequest(4L, List.of()), "admin"));
        verifyNoInteractions(ingredients, lines, users);
    }

    @Test
    void atomicallyReplacesExistingRecipeAfterMatchingVersion() {
        Recipe recipe = storedRecipe();
        when(products.findById(7L)).thenReturn(product());
        when(recipes.findByProductId(7L)).thenReturn(Optional.of(recipe));
        when(recipes.findByIdForUpdate(12L)).thenReturn(Optional.of(recipe));
        when(ingredients.findByIdForUpdate(3L)).thenReturn(Optional.of(ingredient(IngredientUnit.KG)));
        when(users.currentUser("admin")).thenReturn(user());
        when(recipes.saveAndFlush(recipe)).thenReturn(recipe);

        service.save(RecipeOwnerType.PRODUCT, 7L,
                new RecipeSaveRequest(0L, List.of(
                        new RecipeLineRequest(3L, new BigDecimal("0.100")))), "admin");

        verify(lines).deleteByRecipeId(12L);
        verify(lines).saveAllAndFlush(org.mockito.ArgumentMatchers.anyList());
    }

    private Recipe storedRecipe() {
        Recipe recipe = new Recipe(RecipeOwnerType.PRODUCT, 7L, 4L, "admin");
        ReflectionTestUtils.setField(recipe, "id", 12L);
        return recipe;
    }

    private Ingredient ingredient(IngredientUnit unit) {
        return new Ingredient(new IngredientRequest("Carne", unit, BigDecimal.ZERO, null));
    }

    private ProductResponse product() {
        Instant now = Instant.now();
        return new ProductResponse(7L, 1L, "Platillos", null, "Taco", null,
                BigDecimal.TEN, ProductDestination.PRODUCTION, true, true, 0L, now, now);
    }

    private UserResponse user() {
        Instant now = Instant.now();
        return new UserResponse(4L, "admin", "Administrador", UserRole.ADMIN,
                true, false, 0L, now, now);
    }
}
