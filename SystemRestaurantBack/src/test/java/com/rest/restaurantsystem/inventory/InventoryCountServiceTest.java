package com.rest.restaurantsystem.inventory;

import com.rest.restaurantsystem.exception.ConflictException;
import com.rest.restaurantsystem.realtime.RealtimeEventPublisher;
import com.rest.restaurantsystem.structure.BranchService;
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
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InventoryCountServiceTest {
    @Mock IngredientRepository ingredients;
    @Mock InventoryMovementRepository movements;
    @Mock InventoryCountRepository counts;
    @Mock InventoryCountLineRepository lines;
    @Mock UserService users;
    @Mock BranchService branches;
    @Mock RealtimeEventPublisher realtime;
    @InjectMocks InventoryCountService service;

    @Test
    void reconcilesOnlyDifferencesAndKeepsTheOriginalCost() {
        Ingredient flour = ingredient(1L, "Harina", "2.000", "40.2500");
        Ingredient milk = ingredient(2L, "Leche", "1.000", null);
        when(ingredients.findAllByOrderByNameAsc()).thenReturn(List.of(flour, milk));
        when(ingredients.findByIdForUpdate(1L)).thenReturn(Optional.of(flour));
        when(ingredients.findByIdForUpdate(2L)).thenReturn(Optional.of(milk));
        when(users.currentUser("admin")).thenReturn(user());
        when(counts.saveAndFlush(any(InventoryCount.class))).thenAnswer(call -> {
            InventoryCount count = call.getArgument(0);
            ReflectionTestUtils.setField(count, "id", 42L);
            count.onCreate();
            return count;
        });
        when(movements.saveAndFlush(any(InventoryMovement.class))).thenAnswer(call -> {
            InventoryMovement movement = call.getArgument(0);
            ReflectionTestUtils.setField(movement, "id", 77L);
            return movement;
        });
        when(lines.saveAndFlush(any(InventoryCountLine.class))).thenAnswer(call -> call.getArgument(0));

        InventoryCountResponse result = service.reconcile(request("1.000", "1.000", 0, 0), "admin");

        assertEquals(1, result.adjustedLines());
        assertEquals(new BigDecimal("-40.2500000"), result.knownDifferenceValue());
        assertEquals(0, result.unpricedDifferences());
        assertEquals(new BigDecimal("1.000"), flour.getStockQuantity());
        assertEquals(new BigDecimal("1.000"), milk.getStockQuantity());
        assertEquals(77L, result.lines().getFirst().movementId());
        verify(movements).saveAndFlush(any(InventoryMovement.class));
    }

    @Test
    void staleVersionRejectsWholeCountWithoutPostingAnyMovement() {
        Ingredient flour = ingredient(1L, "Harina", "2.000", null);
        Ingredient milk = ingredient(2L, "Leche", "1.000", null);
        when(ingredients.findAllByOrderByNameAsc()).thenReturn(List.of(flour, milk));
        when(ingredients.findByIdForUpdate(1L)).thenReturn(Optional.of(flour));
        when(ingredients.findByIdForUpdate(2L)).thenReturn(Optional.of(milk));

        assertThrows(ConflictException.class,
                () -> service.reconcile(request("1.000", "0.000", 0, 99), "admin"));
        assertEquals(new BigDecimal("2.000"), flour.getStockQuantity());
        assertEquals(new BigDecimal("1.000"), milk.getStockQuantity());
        verify(counts, never()).saveAndFlush(any());
        verify(movements, never()).saveAndFlush(any());
    }

    @Test
    void retryingSameOperationReturnsOriginalResult() {
        InventoryCountRequest request = request("1.000", "1.000", 0, 0);
        InventoryCount previous = new InventoryCount(request.operationId(), request.reason(), 4L, "admin");
        ReflectionTestUtils.setField(previous, "id", 42L);
        previous.onCreate();
        Ingredient flour = ingredient(1L, "Harina", "2.000", null);
        Ingredient milk = ingredient(2L, "Leche", "1.000", null);
        when(counts.findByOperationId(request.operationId())).thenReturn(Optional.of(previous));
        when(lines.findByCountIdOrderByIngredientIdAsc(42L)).thenReturn(List.of(
                new InventoryCountLine(42L, flour, 0, new BigDecimal("2.000"), new BigDecimal("1.000"), 77L),
                new InventoryCountLine(42L, milk, 0, new BigDecimal("1.000"), new BigDecimal("1.000"), null)));

        assertEquals(42L, service.reconcile(request, "admin").id());
        verify(ingredients, never()).findAllByOrderByNameAsc();
        verify(movements, never()).saveAndFlush(any());
    }

    private Ingredient ingredient(Long id, String name, String stock, String cost) {
        Ingredient result = new Ingredient(new IngredientRequest(name, IngredientUnit.KG,
                BigDecimal.ONE, null));
        ReflectionTestUtils.setField(result, "id", id);
        result.setStockQuantity(new BigDecimal(stock));
        if (cost != null) result.setUnitCost(new BigDecimal(cost));
        return result;
    }

    private InventoryCountRequest request(String first, String second, long firstVersion, long secondVersion) {
        return new InventoryCountRequest(UUID.randomUUID(), "Conteo semanal", List.of(
                new InventoryCountRequest.Line(1L, firstVersion, new BigDecimal(first)),
                new InventoryCountRequest.Line(2L, secondVersion, new BigDecimal(second))));
    }

    private UserResponse user() {
        Instant now = Instant.now();
        return new UserResponse(4L, "admin", "Administración", UserRole.ADMIN,
                true, false, 0L, now, now);
    }
}
