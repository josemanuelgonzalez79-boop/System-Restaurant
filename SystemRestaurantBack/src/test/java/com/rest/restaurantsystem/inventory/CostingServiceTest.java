package com.rest.restaurantsystem.inventory;

import com.rest.restaurantsystem.catalog.modifier.ModifierGroupResponse;
import com.rest.restaurantsystem.catalog.modifier.ModifierGroupService;
import com.rest.restaurantsystem.catalog.modifier.ModifierOptionResponse;
import com.rest.restaurantsystem.catalog.product.ProductDestination;
import com.rest.restaurantsystem.catalog.product.ProductResponse;
import com.rest.restaurantsystem.catalog.product.ProductService;
import com.rest.restaurantsystem.exception.ConflictException;
import com.rest.restaurantsystem.realtime.RealtimeEventPublisher;
import com.rest.restaurantsystem.structure.BranchService;
import com.rest.restaurantsystem.user.UserResponse;
import com.rest.restaurantsystem.user.UserRole;
import com.rest.restaurantsystem.user.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CostingServiceTest {
    @Mock IngredientRepository ingredients;
    @Mock RecipeRepository recipes;
    @Mock RecipeLineRepository recipeLines;
    @Mock CostChangeRepository changes;
    @Mock ProductService products;
    @Mock ModifierGroupService modifierGroups;
    @Mock UserService users;
    @Mock BranchService branches;
    @Mock RealtimeEventPublisher events;
    @InjectMocks CostingService service;

    @Test
    void calculatesBaseAndExtraMarginsSeparately() {
        Ingredient meat = ingredient(3L, "Carne", new BigDecimal("100.0000"));
        Ingredient cheese = ingredient(4L, "Queso", new BigDecimal("200.0000"));
        Recipe base = recipe(10L, RecipeOwnerType.PRODUCT, 7L);
        Recipe extra = recipe(11L, RecipeOwnerType.MODIFIER_OPTION, 9L);
        ModifierOptionResponse option = new ModifierOptionResponse(9L, 8L, "Extras", 7L,
                "Queso extra", new BigDecimal("5.00"), 0, true, 0, Instant.EPOCH, Instant.EPOCH);
        when(ingredients.findAll()).thenReturn(List.of(meat, cheese));
        when(recipes.findAll()).thenReturn(List.of(base, extra));
        when(recipeLines.findAll()).thenReturn(List.of(
                new RecipeLine(10L, 3L, new BigDecimal("0.150")),
                new RecipeLine(11L, 4L, new BigDecimal("0.020"))));
        when(products.findAll(null)).thenReturn(List.of(product()));
        when(modifierGroups.findAll(null)).thenReturn(List.of(new ModifierGroupResponse(
                8L, 7L, "Hamburguesa", "Extras", 0, 1, 0, true, 0,
                Instant.EPOCH, Instant.EPOCH, List.of(option))));

        ProductCostResponse result = service.catalog().getFirst();

        assertThat(result.recipe().cost()).isEqualByComparingTo("15.00");
        assertThat(result.grossProfit()).isEqualByComparingTo("35.00");
        assertThat(result.marginPercent()).isEqualByComparingTo("70.0");
        assertThat(result.options().getFirst().recipe().cost()).isEqualByComparingTo("4.00");
        assertThat(result.options().getFirst().marginPercent()).isEqualByComparingTo("20.0");
    }

    @Test
    void missingIngredientCostKeepsMarginUnknown() {
        when(ingredients.findAll()).thenReturn(List.of(ingredient(3L, "Carne", null)));
        when(recipes.findAll()).thenReturn(List.of(recipe(10L, RecipeOwnerType.PRODUCT, 7L)));
        when(recipeLines.findAll()).thenReturn(List.of(
                new RecipeLine(10L, 3L, new BigDecimal("0.150"))));
        when(products.findAll(null)).thenReturn(List.of(product()));
        when(modifierGroups.findAll(null)).thenReturn(List.of());

        ProductCostResponse result = service.catalog().getFirst();

        assertThat(result.recipe().configured()).isTrue();
        assertThat(result.recipe().cost()).isNull();
        assertThat(result.recipe().missingCosts()).containsExactly("Carne");
        assertThat(result.marginPercent()).isNull();
    }

    @Test
    void changingUnitCostWritesAnAuditedRecordAndRejectsStaleVersion() {
        Ingredient ingredient = ingredient(3L, "Carne", null);
        when(ingredients.findByIdForUpdate(3L)).thenReturn(Optional.of(ingredient));
        when(ingredients.saveAndFlush(ingredient)).thenReturn(ingredient);
        when(users.currentUser("admin")).thenReturn(new UserResponse(4L, "admin", "Admin",
                UserRole.ADMIN, true, false, 0, Instant.EPOCH, Instant.EPOCH));

        IngredientResponse saved = service.updateCost(3L,
                new CostUpdateRequest(new BigDecimal("125.5000"), 0L, "Nuevo proveedor"), "admin");

        assertThat(saved.unitCost()).isEqualByComparingTo("125.5000");
        ArgumentCaptor<CostChange> recorded = ArgumentCaptor.forClass(CostChange.class);
        verify(changes).saveAndFlush(recorded.capture());
        assertThat(recorded.getValue().getPreviousUnitCost()).isNull();
        assertThat(recorded.getValue().getNewUnitCost()).isEqualByComparingTo("125.5000");
        assertThat(recorded.getValue().getReason()).isEqualTo("Nuevo proveedor");

        assertThrows(ConflictException.class, () -> service.updateCost(3L,
                new CostUpdateRequest(new BigDecimal("130.0000"), 2L, "Otra factura"), "admin"));
        verify(changes, times(1)).saveAndFlush(any(CostChange.class));
    }

    private Ingredient ingredient(Long id, String name, BigDecimal cost) {
        Ingredient ingredient = new Ingredient(new IngredientRequest(name, IngredientUnit.KG,
                BigDecimal.ZERO, null));
        ReflectionTestUtils.setField(ingredient, "id", id);
        ingredient.setUnitCost(cost);
        return ingredient;
    }

    private Recipe recipe(Long id, RecipeOwnerType type, Long ownerId) {
        Recipe recipe = new Recipe(type, ownerId, 4L, "admin");
        ReflectionTestUtils.setField(recipe, "id", id);
        return recipe;
    }

    private ProductResponse product() {
        return new ProductResponse(7L, 2L, "Platillos", "HAM-01", "Hamburguesa", null,
                new BigDecimal("50.00"), ProductDestination.PRODUCTION, true, true, 0,
                Instant.EPOCH, Instant.EPOCH);
    }
}
