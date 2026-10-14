package com.rest.restaurantsystem.inventory;

import com.rest.restaurantsystem.exception.BadRequestException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderConsumptionServiceTest {

    @Mock RecipeRepository recipes;
    @Mock RecipeLineRepository recipeLines;
    @Mock IngredientRepository ingredients;
    @Mock InventoryMovementRepository movements;
    @InjectMocks OrderConsumptionService service;

    @Test
    void sumsProductAndExtraPerUnitAndWritesOneAuditableDebit() {
        Recipe product = recipe(12L, RecipeOwnerType.PRODUCT, 7L);
        Recipe extra = recipe(13L, RecipeOwnerType.MODIFIER_OPTION, 9L);
        Ingredient ingredient = ingredient("Carne", "5.000");
        ingredient.setUnitCost(new BigDecimal("100.0000"));
        when(recipes.findByProductId(7L)).thenReturn(Optional.of(product));
        when(recipes.findByModifierOptionId(9L)).thenReturn(Optional.of(extra));
        when(recipes.findByIdForUpdate(12L)).thenReturn(Optional.of(product));
        when(recipes.findByIdForUpdate(13L)).thenReturn(Optional.of(extra));
        when(recipeLines.findByRecipeIdOrderByIdAsc(12L)).thenReturn(List.of(line(12L, "0.150")));
        when(recipeLines.findByRecipeIdOrderByIdAsc(13L)).thenReturn(List.of(line(13L, "0.020")));
        when(ingredients.findByIdForUpdate(3L)).thenReturn(Optional.of(ingredient));

        service.consume(List.of(new ConsumptionRequest(50L, 42L, 7L, "Hamburguesa", 2, List.of(9L))),
                4L, "mesero");

        assertThat(ingredient.getStockQuantity()).isEqualByComparingTo("4.660");
        ArgumentCaptor<InventoryMovement> saved = ArgumentCaptor.forClass(InventoryMovement.class);
        verify(movements).saveAndFlush(saved.capture());
        assertThat(saved.getValue().getQuantityDelta()).isEqualByComparingTo("-0.340");
        assertThat(saved.getValue().getPreparationItemId()).isEqualTo(50L);
        assertThat(saved.getValue().getMovementType()).isEqualTo(MovementType.ORDER_CONSUMPTION);
        assertThat(saved.getValue().getUnitCostSnapshot()).isEqualByComparingTo("100.0000");
        assertThat(saved.getValue().getCostAmountSnapshot()).isEqualByComparingTo("-34.0000000");
    }

    @Test
    void rejectsInsufficientStockBeforeCreatingAnyMovement() {
        Recipe product = recipe(12L, RecipeOwnerType.PRODUCT, 7L);
        when(recipes.findByProductId(7L)).thenReturn(Optional.of(product));
        when(recipes.findByIdForUpdate(12L)).thenReturn(Optional.of(product));
        when(recipeLines.findByRecipeIdOrderByIdAsc(12L)).thenReturn(List.of(line(12L, "0.150")));
        when(ingredients.findByIdForUpdate(3L)).thenReturn(Optional.of(ingredient("Carne", "0.200")));

        assertThrows(BadRequestException.class, () -> service.consume(
                List.of(
                        new ConsumptionRequest(50L, 42L, 7L, "Hamburguesa", 1, List.of()),
                        new ConsumptionRequest(51L, 42L, 7L, "Hamburguesa", 1, List.of())
                ),
                4L, "mesero"));

        verify(movements, never()).saveAndFlush(any(InventoryMovement.class));
    }

    @Test
    void returnsTheOriginalSnapshotEvenWhenRecipeChangesLater() {
        Ingredient ingredient = ingredient("Carne", "1.000");
        ingredient.setUnitCost(new BigDecimal("200.0000"));
        InventoryMovement consumed = new InventoryMovement(
                UUID.randomUUID(), 3L, MovementType.ORDER_CONSUMPTION,
                new BigDecimal("-0.340"), new BigDecimal("4.660"),
                "Comanda: Hamburguesa", 4L, "mesero", 50L, null,
                new BigDecimal("100.0000"), new BigDecimal("-34.0000000"));
        ReflectionTestUtils.setField(consumed, "id", 31L);
        when(movements.findByPreparationItemIdAndMovementTypeOrderByIngredientIdAsc(
                50L, MovementType.ORDER_CONSUMPTION)).thenReturn(List.of(consumed));
        when(ingredients.findByIdForUpdate(3L)).thenReturn(Optional.of(ingredient));

        service.reversePending(50L, "Cliente canceló", 5L, "cocinero");

        assertThat(ingredient.getStockQuantity()).isEqualByComparingTo("1.340");
        ArgumentCaptor<InventoryMovement> saved = ArgumentCaptor.forClass(InventoryMovement.class);
        verify(movements).saveAndFlush(saved.capture());
        assertThat(saved.getValue().getQuantityDelta()).isEqualByComparingTo("0.340");
        assertThat(saved.getValue().getReversesMovementId()).isEqualTo(31L);
        assertThat(saved.getValue().getUnitCostSnapshot()).isEqualByComparingTo("100.0000");
        assertThat(saved.getValue().getCostAmountSnapshot()).isEqualByComparingTo("34.0000000");
    }

    private Recipe recipe(Long id, RecipeOwnerType type, Long ownerId) {
        Recipe recipe = new Recipe(type, ownerId, 4L, "admin");
        ReflectionTestUtils.setField(recipe, "id", id);
        return recipe;
    }

    private RecipeLine line(Long recipeId, String quantity) {
        return new RecipeLine(recipeId, 3L, new BigDecimal(quantity));
    }

    private Ingredient ingredient(String name, String stock) {
        Ingredient ingredient = new Ingredient(new IngredientRequest(
                name, IngredientUnit.KG, BigDecimal.ZERO, null));
        ReflectionTestUtils.setField(ingredient, "id", 3L);
        ingredient.setStockQuantity(new BigDecimal(stock));
        return ingredient;
    }
}
