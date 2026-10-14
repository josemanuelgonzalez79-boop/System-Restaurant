package com.rest.restaurantsystem.inventory;

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
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class InventoryCountService {
    private final IngredientRepository ingredients;
    private final InventoryMovementRepository movements;
    private final InventoryCountRepository counts;
    private final InventoryCountLineRepository lines;
    private final UserService users;
    private final BranchService branches;
    private final RealtimeEventPublisher realtime;

    public InventoryCountService(IngredientRepository ingredients, InventoryMovementRepository movements,
                                 InventoryCountRepository counts, InventoryCountLineRepository lines,
                                 UserService users, BranchService branches, RealtimeEventPublisher realtime) {
        this.ingredients = ingredients;
        this.movements = movements;
        this.counts = counts;
        this.lines = lines;
        this.users = users;
        this.branches = branches;
        this.realtime = realtime;
    }

    @Transactional(readOnly = true)
    public List<InventoryCountResponse> recent() {
        return counts.findTop20ByOrderByCountedAtDescIdDesc().stream()
                .map(count -> InventoryCountResponse.from(count,
                        lines.findByCountIdOrderByIngredientIdAsc(count.getId()))).toList();
    }

    @Transactional(readOnly = true)
    public InventoryCountResponse find(Long id) {
        InventoryCount count = counts.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró el conteo."));
        return InventoryCountResponse.from(count, lines.findByCountIdOrderByIngredientIdAsc(id));
    }

    @Transactional
    public InventoryCountResponse reconcile(InventoryCountRequest request, String username) {
        String reason = request.reason().trim();
        if (reason.length() < 5) {
            throw new BadRequestException("Indica un motivo de al menos 5 caracteres para el conteo.");
        }
        Map<Long, InventoryCountRequest.Line> submitted = new HashMap<>();
        for (InventoryCountRequest.Line line : request.lines()) {
            if (submitted.putIfAbsent(line.ingredientId(), line) != null) {
                throw new BadRequestException("Hay insumos repetidos en el conteo.");
            }
        }
        InventoryCount existing = counts.findByOperationId(request.operationId()).orElse(null);
        if (existing != null) return retry(existing, submitted, reason, username);

        // Lock in a stable order, shared with the regular movement endpoint. Validate ALL rows
        // before touching any stock, so a failed count never applies only half its adjustments.
        List<Long> activeIds = ingredients.findAllByOrderByNameAsc().stream()
                .filter(Ingredient::isActive).map(Ingredient::getId).sorted().toList();
        if (activeIds.isEmpty()) {
            throw new BadRequestException("Registra al menos un insumo activo antes de contar.");
        }
        if (!submitted.keySet().equals(new java.util.HashSet<>(activeIds))) {
            throw new ConflictException("Los insumos activos cambiaron. Inicia un conteo nuevo.");
        }

        List<Ingredient> locked = activeIds.stream().map(id -> ingredients.findByIdForUpdate(id)
                .orElseThrow(() -> new ConflictException("Un insumo del conteo ya no existe."))).toList();
        existing = counts.findByOperationId(request.operationId()).orElse(null);
        if (existing != null) return retry(existing, submitted, reason, username);
        for (Ingredient ingredient : locked) {
            InventoryCountRequest.Line line = submitted.get(ingredient.getId());
            BigDecimal counted = line.countedQuantity();
            if (!ingredient.isActive() || ingredient.getVersion() != line.expectedVersion()) {
                throw new ConflictException("Cambió el inventario de " + ingredient.getName()
                        + ". Revisa existencias e inicia un conteo nuevo.");
            }
            if (counted.signum() < 0 || counted.scale() > 3 || counted.precision() - counted.scale() > 11
                    || ingredient.getUnit() == IngredientUnit.PIECE
                    && counted.stripTrailingZeros().scale() > 0) {
                throw new BadRequestException("Cantidad inválida en " + ingredient.getName() + ".");
            }
        }

        Long userId = users.currentUser(username).id();
        InventoryCount count = counts.saveAndFlush(
                new InventoryCount(request.operationId(), reason, userId, username));
        List<InventoryCountLine> saved = new java.util.ArrayList<>();
        for (Ingredient ingredient : locked) {
            BigDecimal expected = ingredient.getStockQuantity();
            long version = ingredient.getVersion();
            BigDecimal counted = submitted.get(ingredient.getId()).countedQuantity().setScale(3);
            BigDecimal delta = counted.subtract(expected);
            Long movementId = null;
            if (delta.signum() != 0) {
                ingredient.setStockQuantity(counted);
                ingredients.saveAndFlush(ingredient);
                UUID movementOperation = UUID.nameUUIDFromBytes(
                        ("inventory-count:" + count.getId() + ":" + ingredient.getId())
                                .getBytes(StandardCharsets.UTF_8));
                InventoryMovement movement = movements.saveAndFlush(new InventoryMovement(
                        movementOperation, ingredient.getId(), MovementType.ADJUSTMENT, delta,
                        counted, "Ajuste de conteo físico #" + count.getId(), userId, username));
                movementId = movement.getId();
            }
            saved.add(lines.saveAndFlush(new InventoryCountLine(count.getId(), ingredient,
                    version, expected, counted, movementId)));
        }
        branches.findAll().forEach(branch -> realtime.publish(
                RealtimeEventType.INVENTORY_CHANGED, branch.id(), null, null, null));
        return InventoryCountResponse.from(count, saved);
    }

    private InventoryCountResponse retry(InventoryCount count,
                                         Map<Long, InventoryCountRequest.Line> submitted,
                                         String reason, String username) {
        List<InventoryCountLine> previous = lines.findByCountIdOrderByIngredientIdAsc(count.getId());
        boolean same = count.getRecordedByUsername().equals(username) && count.getReason().equals(reason)
                && previous.size() == submitted.size() && previous.stream().allMatch(line -> {
                    InventoryCountRequest.Line item = submitted.get(line.getIngredientId());
                    return item != null && item.expectedVersion() == line.getExpectedVersion()
                            && item.countedQuantity().compareTo(line.getCountedQuantity()) == 0;
                });
        if (!same) throw new ConflictException("Este identificador de conteo ya se utilizó con otros datos.");
        return InventoryCountResponse.from(count, previous);
    }
}
