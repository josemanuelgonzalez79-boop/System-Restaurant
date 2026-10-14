package com.rest.restaurantsystem.inventory;

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

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock IngredientRepository ingredients;
    @Mock InventoryMovementRepository movements;
    @Mock RecipeLineRepository recipeLines;
    @Mock UserService users;
    @InjectMocks InventoryService service;

    @Test
    void recordsPurchaseWithExactThreeDecimalStock() {
        Ingredient ingredient = new Ingredient(request(IngredientUnit.KG));
        when(ingredients.findByIdForUpdate(1L)).thenReturn(Optional.of(ingredient));
        when(ingredients.saveAndFlush(ingredient)).thenReturn(ingredient);
        when(users.currentUser("admin")).thenReturn(user());
        when(movements.saveAndFlush(any(InventoryMovement.class))).thenAnswer(call -> call.getArgument(0));

        InventoryMovementResponse result = service.recordMovement(1L,
                movement(MovementType.PURCHASE, "2.125", 0), "admin");

        assertEquals(new BigDecimal("2.125"), result.stockAfter());
        assertEquals(new BigDecimal("2.125"), result.quantityDelta());
        assertEquals(new BigDecimal("2.125"), ingredient.getStockQuantity());
    }

    @Test
    void rejectsWasteThatWouldMakeStockNegative() {
        Ingredient ingredient = new Ingredient(request(IngredientUnit.KG));
        when(ingredients.findByIdForUpdate(1L)).thenReturn(Optional.of(ingredient));

        assertThrows(BadRequestException.class,
                () -> service.recordMovement(1L, movement(MovementType.WASTE, "0.250", 0), "admin"));
        assertEquals(0, ingredient.getStockQuantity().signum());
    }

    @Test
    void rejectsStaleTabletVersion() {
        when(ingredients.findByIdForUpdate(1L))
                .thenReturn(Optional.of(new Ingredient(request(IngredientUnit.KG))));

        assertThrows(ConflictException.class,
                () -> service.recordMovement(1L, movement(MovementType.PURCHASE, "1", 7), "admin"));
    }

    @Test
    void rejectsFractionalPieces() {
        when(ingredients.findByIdForUpdate(1L))
                .thenReturn(Optional.of(new Ingredient(request(IngredientUnit.PIECE))));

        assertThrows(BadRequestException.class,
                () -> service.recordMovement(1L, movement(MovementType.PURCHASE, "0.5", 0), "admin"));
    }

    @Test
    void retriesSameOperationWithoutChangingStockAgain() {
        UUID operationId = UUID.randomUUID();
        InventoryMovement saved = new InventoryMovement(operationId, 1L, MovementType.PURCHASE,
                new BigDecimal("2.000"), new BigDecimal("2.000"), "Compra", 4L, "admin");
        when(movements.findByOperationId(operationId)).thenReturn(Optional.of(saved));

        InventoryMovementResponse result = service.recordMovement(1L,
                new StockMovementRequest(operationId, MovementType.PURCHASE,
                        new BigDecimal("2"), "Compra", 0L), "admin");

        assertEquals(new BigDecimal("2.000"), result.stockAfter());
        verifyNoInteractions(ingredients, users);
    }

    @Test
    void rejectsReusedOperationIdWithDifferentAmount() {
        UUID operationId = UUID.randomUUID();
        InventoryMovement saved = new InventoryMovement(operationId, 1L, MovementType.PURCHASE,
                new BigDecimal("2.000"), new BigDecimal("2.000"), "Compra", 4L, "admin");
        when(movements.findByOperationId(operationId)).thenReturn(Optional.of(saved));

        assertThrows(ConflictException.class, () -> service.recordMovement(1L,
                new StockMovementRequest(operationId, MovementType.PURCHASE,
                        new BigDecimal("3"), "Compra", 0L), "admin"));
    }

    private IngredientRequest request(IngredientUnit unit) {
        return new IngredientRequest("Harina", unit, BigDecimal.ONE, null);
    }

    private StockMovementRequest movement(MovementType type, String quantity, long version) {
        return new StockMovementRequest(UUID.randomUUID(), type,
                new BigDecimal(quantity), "Compra", version);
    }

    private UserResponse user() {
        Instant now = Instant.now();
        return new UserResponse(4L, "admin", "Administración", UserRole.ADMIN,
                true, false, 0L, now, now);
    }
}
