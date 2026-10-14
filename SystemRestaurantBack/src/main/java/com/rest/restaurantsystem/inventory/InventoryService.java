package com.rest.restaurantsystem.inventory;

import com.rest.restaurantsystem.exception.BadRequestException;
import com.rest.restaurantsystem.exception.ConflictException;
import com.rest.restaurantsystem.exception.ResourceNotFoundException;
import com.rest.restaurantsystem.user.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class InventoryService {

    private final IngredientRepository ingredients;
    private final InventoryMovementRepository movements;
    private final RecipeLineRepository recipeLines;
    private final UserService users;

    public InventoryService(
            IngredientRepository ingredients,
            InventoryMovementRepository movements,
            RecipeLineRepository recipeLines,
            UserService users
    ) {
        this.ingredients = ingredients;
        this.movements = movements;
        this.recipeLines = recipeLines;
        this.users = users;
    }

    @Transactional(readOnly = true)
    public List<IngredientResponse> findAll() {
        return ingredients.findAllByOrderByNameAsc().stream().map(IngredientResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<InventoryMovementResponse> findMovements(Long ingredientId) {
        getIngredient(ingredientId);
        return movements.findTop50ByIngredientIdOrderByOccurredAtDescIdDesc(ingredientId)
                .stream().map(InventoryMovementResponse::from).toList();
    }

    @Transactional
    public IngredientResponse create(IngredientRequest request) {
        validateMinimum(request);
        String name = request.name().trim();
        if (ingredients.existsByNameIgnoreCase(name)) {
            throw new ConflictException("Ya existe un insumo con ese nombre.");
        }
        return IngredientResponse.from(ingredients.saveAndFlush(new Ingredient(request)));
    }

    @Transactional
    public IngredientResponse update(Long id, IngredientRequest request) {
        validateMinimum(request);
        Ingredient ingredient = getLockedIngredient(id);
        if (request.version() == null || request.version() != ingredient.getVersion()) {
            throw new ConflictException("El insumo cambió en otra pantalla. Actualiza la lista e intenta de nuevo.");
        }
        if (ingredients.existsByNameIgnoreCaseAndIdNot(request.name().trim(), id)) {
            throw new ConflictException("Ya existe otro insumo con ese nombre.");
        }
        if (ingredient.getUnit() != request.unit()
                && (ingredient.getStockQuantity().signum() > 0
                || movements.existsByIngredientId(id) || recipeLines.existsByIngredientId(id))) {
            throw new BadRequestException("No puedes cambiar la unidad de un insumo con movimientos o recetas.");
        }
        ingredient.update(request);
        return IngredientResponse.from(ingredients.saveAndFlush(ingredient));
    }

    @Transactional
    public IngredientResponse changeActive(Long id, boolean active) {
        Ingredient ingredient = getLockedIngredient(id);
        if (!active && ingredient.getStockQuantity().signum() > 0) {
            throw new BadRequestException("Registra la salida del inventario antes de desactivar el insumo.");
        }
        if (!active && recipeLines.existsByIngredientId(id)) {
            throw new BadRequestException("Quita este insumo de las recetas antes de desactivarlo.");
        }
        ingredient.setActive(active);
        return IngredientResponse.from(ingredients.saveAndFlush(ingredient));
    }

    @Transactional
    public InventoryMovementResponse recordMovement(Long ingredientId, StockMovementRequest request, String username) {
        InventoryMovement existing = movements.findByOperationId(request.operationId()).orElse(null);
        if (existing != null) {
            return retryResult(existing, ingredientId, request, username);
        }

        // The row lock serializes stock updates from two tablets and protects the idempotency check.
        Ingredient ingredient = ingredients.findByIdForUpdate(ingredientId)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró el insumo."));
        existing = movements.findByOperationId(request.operationId()).orElse(null);
        if (existing != null) {
            return retryResult(existing, ingredientId, request, username);
        }
        if (!ingredient.isActive()) {
            throw new BadRequestException("Activa el insumo antes de registrar movimientos.");
        }
        if (ingredient.getVersion() != request.expectedVersion()) {
            throw new ConflictException("El inventario cambió en otra tablet. Actualiza las existencias antes de guardar.");
        }

        BigDecimal quantity = request.quantity().setScale(3);
        if (ingredient.getUnit() == IngredientUnit.PIECE && quantity.stripTrailingZeros().scale() > 0) {
            throw new BadRequestException("Las piezas se registran en números enteros.");
        }
        BigDecimal before = ingredient.getStockQuantity();
        BigDecimal delta;
        if (request.movementType() == MovementType.ADJUSTMENT) {
            delta = quantity.subtract(before);
        } else {
            if (quantity.signum() <= 0) {
                throw new BadRequestException("La cantidad del movimiento debe ser mayor a cero.");
            }
            delta = request.movementType() == MovementType.PURCHASE ? quantity : quantity.negate();
        }
        if (delta.signum() == 0) {
            throw new BadRequestException("La nueva existencia debe ser diferente de la actual.");
        }
        BigDecimal after = before.add(delta);
        if (after.signum() < 0) {
            throw new BadRequestException("No hay existencias suficientes para esta salida o merma.");
        }
        if (after.precision() - after.scale() > 11) {
            throw new BadRequestException("La existencia excede el máximo permitido.");
        }
        ingredient.setStockQuantity(after);
        ingredients.saveAndFlush(ingredient);

        Long userId = users.currentUser(username).id();
        InventoryMovement movement = new InventoryMovement(
                request.operationId(), ingredientId, request.movementType(), delta, after,
                request.reason(), userId, username
        );
        return InventoryMovementResponse.from(movements.saveAndFlush(movement));
    }

    private InventoryMovementResponse retryResult(
            InventoryMovement existing, Long ingredientId, StockMovementRequest request, String username
    ) {
        BigDecimal requested = request.quantity().setScale(3);
        BigDecimal previousAmount = existing.getMovementType() == MovementType.ADJUSTMENT
                ? existing.getStockAfter() : existing.getQuantityDelta().abs();
        if (!existing.getIngredientId().equals(ingredientId)
                || existing.getMovementType() != request.movementType()
                || previousAmount.compareTo(requested) != 0
                || !existing.getReason().equals(request.reason().trim())
                || !existing.getRecordedByUsername().equals(username)) {
            throw new ConflictException("El identificador de la operación ya se usó en otro movimiento.");
        }
        return InventoryMovementResponse.from(existing);
    }

    private Ingredient getIngredient(Long id) {
        return ingredients.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró el insumo."));
    }

    private Ingredient getLockedIngredient(Long id) {
        return ingredients.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró el insumo."));
    }

    private void validateMinimum(IngredientRequest request) {
        if (request.unit() == IngredientUnit.PIECE
                && request.minimumQuantity().stripTrailingZeros().scale() > 0) {
            throw new BadRequestException("El mínimo de piezas debe ser un número entero.");
        }
    }
}
