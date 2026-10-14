package com.rest.restaurantsystem.inventory;

import com.rest.restaurantsystem.exception.BadRequestException;
import com.rest.restaurantsystem.exception.ConflictException;
import com.rest.restaurantsystem.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

@Service
public class OrderConsumptionService {

    private final RecipeRepository recipes;
    private final RecipeLineRepository recipeLines;
    private final IngredientRepository ingredients;
    private final InventoryMovementRepository movements;

    public OrderConsumptionService(
            RecipeRepository recipes, RecipeLineRepository recipeLines,
            IngredientRepository ingredients, InventoryMovementRepository movements
    ) {
        this.recipes = recipes;
        this.recipeLines = recipeLines;
        this.ingredients = ingredients;
        this.movements = movements;
    }

    /** Called inside the dispatch transaction. A failure rolls back tickets and stock together. */
    @Transactional
    public void consume(List<ConsumptionRequest> requests, Long userId, String username) {
        if (requests.isEmpty()) {
            return;
        }

        Map<Long, Long> products = new HashMap<>();
        Map<Long, Long> options = new HashMap<>();
        for (ConsumptionRequest request : requests) {
            if (movements.existsByPreparationItemIdAndMovementType(
                    request.preparationItemId(), MovementType.ORDER_CONSUMPTION)) {
                throw new ConflictException("La comanda ya registró su consumo. Actualiza el pedido.");
            }
            products.computeIfAbsent(request.productId(), id -> recipes.findByProductId(id)
                    .map(Recipe::getId)
                    .orElseThrow(() -> new BadRequestException(
                            "Configura la receta de " + request.productName() + " antes de enviar su comanda.")));
            for (Long optionId : request.modifierOptionIds()) {
                recipes.findByModifierOptionId(optionId)
                        .ifPresent(recipe -> options.put(optionId, recipe.getId()));
            }
        }

        // Writers lock the recipe before its ingredients. Keep the same order during dispatch.
        Map<Long, List<RecipeLine>> linesByRecipe = new HashMap<>();
        List<Long> recipeIds = java.util.stream.Stream.concat(products.values().stream(), options.values().stream())
                .distinct().sorted().toList();
        for (Long recipeId : recipeIds) {
            recipes.findByIdForUpdate(recipeId)
                    .orElseThrow(() -> new ConflictException("La receta cambió. Actualiza el pedido."));
            linesByRecipe.put(recipeId, recipeLines.findByRecipeIdOrderByIdAsc(recipeId));
        }

        Map<Long, Map<Long, BigDecimal>> byItem = new LinkedHashMap<>();
        Map<Long, BigDecimal> required = new TreeMap<>();
        for (ConsumptionRequest request : requests) {
            Map<Long, BigDecimal> amounts = new TreeMap<>();
            List<RecipeLine> base = linesByRecipe.get(products.get(request.productId()));
            if (base.isEmpty()) {
                throw new BadRequestException("Agrega insumos a la receta de " + request.productName() + ".");
            }
            addAmounts(amounts, base, request.quantity());
            for (Long optionId : request.modifierOptionIds()) {
                Long recipeId = options.get(optionId);
                if (recipeId != null) {
                    addAmounts(amounts, linesByRecipe.get(recipeId), request.quantity());
                }
            }
            byItem.put(request.preparationItemId(), amounts);
            amounts.forEach((id, amount) -> required.merge(id, amount, BigDecimal::add));
        }

        Map<Long, Ingredient> locked = lockIngredients(new ArrayList<>(required.keySet()));
        for (Map.Entry<Long, BigDecimal> entry : required.entrySet()) {
            Ingredient ingredient = locked.get(entry.getKey());
            if (!ingredient.isActive()) {
                throw new BadRequestException("Activa el insumo " + ingredient.getName() + " antes de enviar.");
            }
            if (ingredient.getStockQuantity().compareTo(entry.getValue()) < 0) {
                throw new BadRequestException("No alcanza " + ingredient.getName() + ": se requieren "
                        + entry.getValue().toPlainString() + " " + ingredient.getUnit()
                        + " y hay " + ingredient.getStockQuantity().toPlainString()
                        + ". Registra una entrada antes de enviar.");
            }
        }

        for (ConsumptionRequest request : requests) {
            for (Map.Entry<Long, BigDecimal> entry : byItem.get(request.preparationItemId()).entrySet()) {
                Ingredient ingredient = locked.get(entry.getKey());
                BigDecimal remaining = ingredient.getStockQuantity().subtract(entry.getValue());
                ingredient.setStockQuantity(remaining);
                ingredients.saveAndFlush(ingredient);
                BigDecimal cost = ingredient.getUnitCost();
                BigDecimal debit = cost == null ? null : entry.getValue().negate().multiply(cost);
                movements.saveAndFlush(new InventoryMovement(
                        operationId(request.preparationItemId(), entry.getKey(), MovementType.ORDER_CONSUMPTION),
                        entry.getKey(), MovementType.ORDER_CONSUMPTION, entry.getValue().negate(),
                        remaining, "PED-%06d · ".formatted(request.orderId())
                                + request.productName(), userId, username,
                        request.preparationItemId(), null, cost, debit
                ));
            }
        }
    }

    /** Recredit the immutable consumption snapshot only when the item never started preparation. */
    @Transactional
    public void reversePending(Long preparationItemId, String reason, Long userId, String username) {
        List<InventoryMovement> consumed = movements
                .findByPreparationItemIdAndMovementTypeOrderByIngredientIdAsc(
                        preparationItemId, MovementType.ORDER_CONSUMPTION);
        if (consumed.isEmpty()) { // An older ticket sent before this feature has no stock debit.
            return;
        }
        if (movements.existsByPreparationItemIdAndMovementType(
                preparationItemId, MovementType.ORDER_REVERSAL)) {
            throw new ConflictException("Esta comanda ya devolvió sus insumos.");
        }
        Map<Long, Ingredient> locked = lockIngredients(consumed.stream()
                .map(InventoryMovement::getIngredientId).distinct().sorted().toList());
        for (InventoryMovement original : consumed) {
            Ingredient ingredient = locked.get(original.getIngredientId());
            BigDecimal quantity = original.getQuantityDelta().negate();
            BigDecimal restored = ingredient.getStockQuantity().add(quantity);
            if (restored.precision() - restored.scale() > 11) {
                throw new BadRequestException("La devolución excede la capacidad del insumo "
                        + ingredient.getName() + ".");
            }
            ingredient.setStockQuantity(restored);
            ingredients.saveAndFlush(ingredient);
            movements.saveAndFlush(new InventoryMovement(
                    operationId(preparationItemId, original.getIngredientId(), MovementType.ORDER_REVERSAL),
                    original.getIngredientId(), MovementType.ORDER_REVERSAL, quantity, restored,
                    "Cancelación antes de preparar: " + reason, userId, username,
                    preparationItemId, original.getId(), original.getUnitCostSnapshot(),
                    original.getCostAmountSnapshot() == null ? null
                            : original.getCostAmountSnapshot().negate()
            ));
        }
    }

    private Map<Long, Ingredient> lockIngredients(List<Long> ingredientIds) {
        Map<Long, Ingredient> locked = new HashMap<>();
        for (Long id : ingredientIds) {
            locked.put(id, ingredients.findByIdForUpdate(id)
                    .orElseThrow(() -> new ResourceNotFoundException("No se encontró el insumo de la receta.")));
        }
        return locked;
    }

    private void addAmounts(Map<Long, BigDecimal> amounts, List<RecipeLine> lines, int quantity) {
        for (RecipeLine line : lines) {
            amounts.merge(line.getIngredientId(),
                    line.getQuantity().multiply(BigDecimal.valueOf(quantity)), BigDecimal::add);
        }
    }

    private UUID operationId(Long preparationItemId, Long ingredientId, MovementType type) {
        return UUID.nameUUIDFromBytes(("essential-plus:" + type + ":" + preparationItemId + ":"
                + ingredientId).getBytes(StandardCharsets.UTF_8));
    }
}
