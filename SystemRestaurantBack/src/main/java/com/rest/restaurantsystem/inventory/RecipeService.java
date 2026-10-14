package com.rest.restaurantsystem.inventory;

import com.rest.restaurantsystem.catalog.modifier.ModifierOptionResponse;
import com.rest.restaurantsystem.catalog.modifier.ModifierOptionService;
import com.rest.restaurantsystem.catalog.product.ProductService;
import com.rest.restaurantsystem.exception.BadRequestException;
import com.rest.restaurantsystem.exception.ConflictException;
import com.rest.restaurantsystem.exception.ResourceNotFoundException;
import com.rest.restaurantsystem.user.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class RecipeService {

    private final RecipeRepository recipes;
    private final RecipeLineRepository lines;
    private final IngredientRepository ingredients;
    private final ProductService products;
    private final ModifierOptionService modifierOptions;
    private final UserService users;

    public RecipeService(
            RecipeRepository recipes, RecipeLineRepository lines, IngredientRepository ingredients,
            ProductService products, ModifierOptionService modifierOptions, UserService users
    ) {
        this.recipes = recipes;
        this.lines = lines;
        this.ingredients = ingredients;
        this.products = products;
        this.modifierOptions = modifierOptions;
        this.users = users;
    }

    @Transactional(readOnly = true)
    public RecipeResponse find(RecipeOwnerType type, Long ownerId) {
        String ownerName = ownerName(type, ownerId);
        Recipe recipe = findRecipe(type, ownerId);
        return response(type, ownerId, ownerName, recipe);
    }

    @Transactional
    public RecipeResponse save(RecipeOwnerType type, Long ownerId, RecipeSaveRequest request, String username) {
        String ownerName = ownerName(type, ownerId);
        Recipe existing = findRecipe(type, ownerId);
        if (existing == null && request.expectedVersion() != null) {
            throw new ConflictException("La receta cambió en otra pantalla. Actualiza antes de guardar.");
        }
        if (existing != null) {
            existing = recipes.findByIdForUpdate(existing.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("No se encontró la receta."));
            if (request.expectedVersion() == null || request.expectedVersion() != existing.getVersion()) {
                throw new ConflictException("La receta cambió en otra pantalla. Actualiza antes de guardar.");
            }
        }

        validateAndLockIngredients(request.lines());
        Long userId = users.currentUser(username).id();
        Recipe recipe;
        if (existing == null) {
            recipe = recipes.saveAndFlush(new Recipe(type, ownerId, userId, username));
        } else {
            existing.updateEditor(userId, username);
            recipe = recipes.saveAndFlush(existing);
            lines.deleteByRecipeId(recipe.getId());
        }
        List<RecipeLine> replacements = request.lines().stream()
                .map(line -> new RecipeLine(recipe.getId(), line.ingredientId(), line.quantity().setScale(3)))
                .toList();
        lines.saveAllAndFlush(replacements);
        return response(type, ownerId, ownerName, recipe);
    }

    private void validateAndLockIngredients(List<RecipeLineRequest> entries) {
        if (entries.size() > 50) {
            throw new BadRequestException("Una receta admite máximo 50 insumos.");
        }
        Set<Long> uniqueIds = new HashSet<>();
        for (RecipeLineRequest entry : entries) {
            if (!uniqueIds.add(entry.ingredientId())) {
                throw new BadRequestException("Un insumo solo puede aparecer una vez en la receta.");
            }
        }
        // Lock in the same order for every recipe so unit changes and deactivation cannot race.
        Map<Long, Ingredient> locked = new HashMap<>();
        uniqueIds.stream().sorted().forEach(id -> locked.put(id,
                ingredients.findByIdForUpdate(id)
                        .orElseThrow(() -> new ResourceNotFoundException("No se encontró el insumo."))));
        for (RecipeLineRequest entry : entries) {
            Ingredient ingredient = locked.get(entry.ingredientId());
            if (!ingredient.isActive()) {
                throw new BadRequestException("Activa " + ingredient.getName() + " antes de usarlo en una receta.");
            }
            BigDecimal quantity = entry.quantity();
            if (quantity.signum() <= 0 || quantity.scale() > 3 || quantity.precision() - quantity.scale() > 11) {
                throw new BadRequestException("La cantidad por unidad debe ser positiva y tener hasta tres decimales.");
            }
            if (ingredient.getUnit() == IngredientUnit.PIECE
                    && quantity.stripTrailingZeros().scale() > 0) {
                throw new BadRequestException("Las piezas se registran en números enteros.");
            }
        }
    }

    private String ownerName(RecipeOwnerType type, Long ownerId) {
        if (type == RecipeOwnerType.PRODUCT) {
            return products.findById(ownerId).name();
        }
        ModifierOptionResponse option = modifierOptions.findById(ownerId);
        return option.groupName() + " · " + option.name();
    }

    private Recipe findRecipe(RecipeOwnerType type, Long ownerId) {
        return (type == RecipeOwnerType.PRODUCT
                ? recipes.findByProductId(ownerId)
                : recipes.findByModifierOptionId(ownerId)).orElse(null);
    }

    private RecipeResponse response(RecipeOwnerType type, Long ownerId, String ownerName, Recipe recipe) {
        if (recipe == null) {
            return new RecipeResponse(null, type, ownerId, ownerName, null, null, null, List.of());
        }
        List<RecipeLine> savedLines = lines.findByRecipeIdOrderByIdAsc(recipe.getId());
        Map<Long, Ingredient> byId = new HashMap<>();
        ingredients.findAllById(savedLines.stream().map(RecipeLine::getIngredientId).toList())
                .forEach(ingredient -> byId.put(ingredient.getId(), ingredient));
        List<RecipeLineResponse> parts = savedLines.stream()
                .map(line -> RecipeLineResponse.from(line, byId.get(line.getIngredientId())))
                .sorted(Comparator.comparing(RecipeLineResponse::ingredientName))
                .toList();
        return new RecipeResponse(recipe.getId(), type, ownerId, ownerName, recipe.getVersion(),
                recipe.getUpdatedByUsername(), recipe.getUpdatedAt(), parts);
    }
}
