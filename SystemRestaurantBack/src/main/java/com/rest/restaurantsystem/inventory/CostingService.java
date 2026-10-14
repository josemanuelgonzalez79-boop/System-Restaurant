package com.rest.restaurantsystem.inventory;

import com.rest.restaurantsystem.catalog.modifier.ModifierGroupResponse;
import com.rest.restaurantsystem.catalog.modifier.ModifierGroupService;
import com.rest.restaurantsystem.catalog.modifier.ModifierOptionResponse;
import com.rest.restaurantsystem.catalog.product.ProductService;
import com.rest.restaurantsystem.exception.BadRequestException;
import com.rest.restaurantsystem.exception.ConflictException;
import com.rest.restaurantsystem.exception.ResourceNotFoundException;
import com.rest.restaurantsystem.realtime.RealtimeEventPublisher;
import com.rest.restaurantsystem.realtime.RealtimeEventType;
import com.rest.restaurantsystem.structure.BranchService;
import com.rest.restaurantsystem.user.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class CostingService {
    private final IngredientRepository ingredients;
    private final RecipeRepository recipes;
    private final RecipeLineRepository recipeLines;
    private final CostChangeRepository changes;
    private final ProductService products;
    private final ModifierGroupService modifierGroups;
    private final UserService users;
    private final BranchService branches;
    private final RealtimeEventPublisher events;

    public CostingService(
            IngredientRepository ingredients, RecipeRepository recipes,
            RecipeLineRepository recipeLines, CostChangeRepository changes,
            ProductService products, ModifierGroupService modifierGroups,
            UserService users, BranchService branches, RealtimeEventPublisher events
    ) {
        this.ingredients = ingredients;
        this.recipes = recipes;
        this.recipeLines = recipeLines;
        this.changes = changes;
        this.products = products;
        this.modifierGroups = modifierGroups;
        this.users = users;
        this.branches = branches;
        this.events = events;
    }

    /** Current ingredient-only estimates. Historical dispatch uses movement snapshots instead. */
    @Transactional(readOnly = true)
    public List<ProductCostResponse> catalog() {
        Map<Long, Ingredient> ingredientById = ingredients.findAll().stream()
                .collect(Collectors.toMap(Ingredient::getId, Function.identity()));
        Map<Long, List<RecipeLine>> linesByRecipe = recipeLines.findAll().stream()
                .collect(Collectors.groupingBy(RecipeLine::getRecipeId));
        Map<Long, RecipeCostResponse> productCosts = new HashMap<>();
        Map<Long, RecipeCostResponse> optionCosts = new HashMap<>();
        for (Recipe recipe : recipes.findAll()) {
            RecipeCostResponse calculated = calculateRecipe(
                    linesByRecipe.getOrDefault(recipe.getId(), List.of()), ingredientById);
            if (recipe.getProductId() != null) {
                productCosts.put(recipe.getProductId(), calculated);
            } else {
                optionCosts.put(recipe.getModifierOptionId(), calculated);
            }
        }

        Map<Long, List<ModifierGroupResponse>> groupsByProduct = modifierGroups.findAll(null).stream()
                .collect(Collectors.groupingBy(ModifierGroupResponse::productId));
        RecipeCostResponse unconfigured = new RecipeCostResponse(false, null, List.of(), List.of());
        return products.findAll(null).stream().map(product -> {
            RecipeCostResponse base = productCosts.getOrDefault(product.id(), unconfigured);
            List<OptionCostResponse> options = groupsByProduct
                    .getOrDefault(product.id(), List.of()).stream()
                    .flatMap(group -> group.options().stream()
                            .map(option -> optionCost(group, option,
                                    optionCosts.getOrDefault(option.id(), unconfigured))))
                    .toList();
            return new ProductCostResponse(product.id(), product.name(), product.categoryName(),
                    product.price(), product.destination(), product.active(), base,
                    grossProfit(product.price(), base.cost()),
                    marginPercent(product.price(), base.cost()), options);
        }).toList();
    }

    @Transactional(readOnly = true)
    public List<CostChangeResponse> history(Long ingredientId) {
        ingredients.findById(ingredientId)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró el insumo."));
        return changes.findTop20ByIngredientIdOrderByChangedAtDescIdDesc(ingredientId)
                .stream().map(CostChangeResponse::from).toList();
    }

    @Transactional
    public IngredientResponse updateCost(Long ingredientId, CostUpdateRequest request, String username) {
        String reason = request.reason().trim();
        if (reason.length() < 5) {
            throw new BadRequestException("Explica el cambio de costo con al menos 5 caracteres.");
        }
        Ingredient ingredient = ingredients.findByIdForUpdate(ingredientId)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró el insumo."));
        if (ingredient.getVersion() != request.expectedVersion()) {
            throw new ConflictException("El insumo cambió en otra tablet. Actualiza e intenta de nuevo.");
        }
        BigDecimal newCost = request.unitCost().setScale(4);
        BigDecimal previous = ingredient.getUnitCost();
        if (previous != null && previous.compareTo(newCost) == 0) {
            throw new BadRequestException("El costo nuevo debe ser diferente del actual.");
        }
        ingredient.setUnitCost(newCost);
        IngredientResponse result = IngredientResponse.from(ingredients.saveAndFlush(ingredient));
        changes.saveAndFlush(new CostChange(ingredientId, previous, newCost, reason,
                users.currentUser(username).id(), username));
        branches.findAll().forEach(branch -> events.publish(
                RealtimeEventType.INVENTORY_CHANGED, branch.id(), null, null, null));
        return result;
    }

    private RecipeCostResponse calculateRecipe(
            List<RecipeLine> recipe, Map<Long, Ingredient> ingredientById
    ) {
        if (recipe.isEmpty()) {
            return new RecipeCostResponse(false, null, List.of(), List.of());
        }
        BigDecimal exactTotal = BigDecimal.ZERO;
        List<String> missing = new ArrayList<>();
        List<CostLineResponse> lines = new ArrayList<>();
        for (RecipeLine line : recipe) {
            Ingredient ingredient = ingredientById.get(line.getIngredientId());
            if (ingredient == null) {
                throw new ResourceNotFoundException("Un insumo de la receta ya no existe.");
            }
            BigDecimal unitCost = ingredient.getUnitCost();
            BigDecimal lineCost = unitCost == null ? null : line.getQuantity().multiply(unitCost);
            if (lineCost == null) {
                missing.add(ingredient.getName());
            } else {
                exactTotal = exactTotal.add(lineCost);
            }
            lines.add(new CostLineResponse(ingredient.getId(), ingredient.getName(),
                    ingredient.getUnit(), line.getQuantity(), unitCost, lineCost));
        }
        return new RecipeCostResponse(true,
                missing.isEmpty() ? exactTotal.setScale(2, RoundingMode.HALF_UP) : null,
                missing, lines);
    }

    private OptionCostResponse optionCost(
            ModifierGroupResponse group, ModifierOptionResponse option, RecipeCostResponse recipe
    ) {
        return new OptionCostResponse(option.id(), group.name(), option.name(),
                option.priceDelta(), group.active() && option.active(), recipe,
                grossProfit(option.priceDelta(), recipe.cost()),
                marginPercent(option.priceDelta(), recipe.cost()));
    }

    private BigDecimal grossProfit(BigDecimal price, BigDecimal cost) {
        return cost == null ? null : price.subtract(cost).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal marginPercent(BigDecimal price, BigDecimal cost) {
        if (cost == null || price.signum() <= 0) {
            return null;
        }
        return price.subtract(cost).multiply(BigDecimal.valueOf(100))
                .divide(price, 1, RoundingMode.HALF_UP);
    }
}
