package com.rest.restaurantsystem.order;

import com.rest.restaurantsystem.catalog.modifier.ModifierGroupResponse;
import com.rest.restaurantsystem.catalog.modifier.ModifierGroupService;
import com.rest.restaurantsystem.catalog.modifier.ModifierOptionResponse;
import com.rest.restaurantsystem.catalog.product.ProductResponse;
import com.rest.restaurantsystem.catalog.product.ProductService;
import com.rest.restaurantsystem.exception.BadRequestException;
import com.rest.restaurantsystem.exception.ConflictException;
import com.rest.restaurantsystem.exception.ResourceNotFoundException;
import com.rest.restaurantsystem.realtime.RealtimeEventPublisher;
import com.rest.restaurantsystem.realtime.RealtimeEventType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class OrderItemService {

    private final OrderItemRepository repository;
    private final OrderItemModifierRepository modifierRepository;
    private final RestaurantOrderRepository orderRepository;
    private final OrderPaymentRepository paymentRepository;
    private final OrderService orderService;
    private final ProductService productService;
    private final ModifierGroupService modifierGroupService;
    private final RealtimeEventPublisher realtimeEventPublisher;

    public OrderItemService(
            OrderItemRepository repository,
            OrderItemModifierRepository modifierRepository,
            RestaurantOrderRepository orderRepository,
            OrderPaymentRepository paymentRepository,
            OrderService orderService,
            ProductService productService,
            ModifierGroupService modifierGroupService,
            RealtimeEventPublisher realtimeEventPublisher
    ) {
        this.repository = repository;
        this.modifierRepository = modifierRepository;
        this.orderRepository = orderRepository;
        this.paymentRepository = paymentRepository;
        this.orderService = orderService;
        this.productService = productService;
        this.modifierGroupService = modifierGroupService;
        this.realtimeEventPublisher = realtimeEventPublisher;
    }

    @Transactional(readOnly = true)
    public OrderDetailResponse findDetail(Long orderId, String currentUsername) {
        RestaurantOrder order = orderService.getAccessibleEntity(orderId, currentUsername);
        return buildDetail(order);
    }

    @Transactional
    public OrderDetailResponse add(
            Long orderId,
            OrderItemCreateRequest request,
            String currentUsername
    ) {
        RestaurantOrder order = editableOrder(orderId, request.orderVersion(), currentUsername);
        ProductResponse product = productService.findOrderableById(request.productId());

        List<ModifierOptionResponse> selectedOptions = validateModifiers(
                product.id(),
                request.modifierOptionIds()
        );
        OrderItem item = repository.saveAndFlush(
                new OrderItem(orderId, product, request.quantity(), request.notes())
        );
        saveModifiers(item.getId(), selectedOptions);

        if (order.getStatus() == OrderStatus.OPEN) {
            order.changeStatus(OrderStatus.IN_PROGRESS);
        } else {
            order.touch();
        }
        orderRepository.saveAndFlush(order);
        OrderDetailResponse response = buildDetail(order);
        publishItemsChanged(order);
        return response;
    }

    @Transactional
    public OrderDetailResponse update(
            Long orderId,
            Long itemId,
            OrderItemUpdateRequest request,
            String currentUsername
    ) {
        RestaurantOrder order = editableOrder(orderId, request.orderVersion(), currentUsername);
        OrderItem item = getItem(orderId, itemId);
        ensureNotSent(item);
        List<OrderItemModifier> currentModifiers = modifierRepository
                .findByOrderItemIdInOrderByOrderItemIdAscIdAsc(List.of(itemId));
        boolean sameModifiers = currentModifiers.size() == request.modifierOptionIds().size()
                && currentModifiers.stream()
                        .map(OrderItemModifier::getModifierOptionId)
                        .collect(Collectors.toSet())
                        .equals(new HashSet<>(request.modifierOptionIds()));
        List<ModifierOptionResponse> selectedOptions = sameModifiers
                ? List.of()
                : validateModifiers(item.getProductId(), request.modifierOptionIds());

        item.update(request.quantity(), request.notes());
        repository.saveAndFlush(item);
        if (!sameModifiers) {
            modifierRepository.deleteByOrderItemId(itemId);
            modifierRepository.flush();
            saveModifiers(itemId, selectedOptions);
        }
        order.touch();
        orderRepository.saveAndFlush(order);
        OrderDetailResponse response = buildDetail(order);
        publishItemsChanged(order);
        return response;
    }

    @Transactional
    public OrderDetailResponse remove(
            Long orderId,
            Long itemId,
            long orderVersion,
            String currentUsername
    ) {
        RestaurantOrder order = editableOrder(orderId, orderVersion, currentUsername);
        OrderItem item = getItem(orderId, itemId);
        ensureNotSent(item);
        repository.delete(item);
        repository.flush();
        order.touch();
        orderRepository.saveAndFlush(order);
        OrderDetailResponse response = buildDetail(order);
        publishItemsChanged(order);
        return response;
    }

    private RestaurantOrder editableOrder(Long orderId, long version, String currentUsername) {
        RestaurantOrder order = orderService.getLockedAccessibleEntity(orderId, currentUsername);
        if (order.getStatus() != OrderStatus.OPEN && order.getStatus() != OrderStatus.IN_PROGRESS) {
            throw new BadRequestException("El pedido ya está cerrado y no puede modificarse.");
        }
        if (paymentRepository.existsByOrderIdAndStatus(orderId, PaymentStatus.ACTIVE)) {
            throw new BadRequestException(
                    "La cuenta tiene cobros registrados. Anúlalos antes de modificar productos."
            );
        }
        if (order.getVersion() != version) {
            throw new ConflictException(
                    "El pedido cambió en otro dispositivo. Actualiza la pantalla e intenta nuevamente."
            );
        }
        return order;
    }

    private OrderItem getItem(Long orderId, Long itemId) {
        OrderItem item = repository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontró el producto capturado."
                ));
        if (!item.getOrderId().equals(orderId)) {
            throw new ResourceNotFoundException("No se encontró el producto capturado.");
        }
        return item;
    }

    private void ensureNotSent(OrderItem item) {
        if (item.getSentAt() != null) {
            throw new BadRequestException(
                    "La partida ya fue enviada a preparación y no puede editarse ni retirarse. "
                            + "Captura una partida adicional o solicita su cancelación en Preparación."
            );
        }
    }

    private List<ModifierOptionResponse> validateModifiers(
            Long productId,
            List<Long> selectedIds
    ) {
        Set<Long> uniqueIds = new HashSet<>(selectedIds);
        if (uniqueIds.size() != selectedIds.size()) {
            throw new BadRequestException("No repitas la misma opción en un producto.");
        }

        List<ModifierGroupResponse> groups = modifierGroupService.findAll(productId).stream()
                .filter(ModifierGroupResponse::active)
                .toList();
        Map<Long, ModifierOptionResponse> availableOptions = groups.stream()
                .flatMap(group -> group.options().stream())
                .filter(ModifierOptionResponse::active)
                .collect(Collectors.toMap(
                        ModifierOptionResponse::id,
                        Function.identity(),
                        (left, right) -> left,
                        LinkedHashMap::new
                ));

        List<ModifierOptionResponse> selected = new ArrayList<>();
        for (Long optionId : selectedIds) {
            ModifierOptionResponse option = availableOptions.get(optionId);
            if (option == null) {
                throw new BadRequestException(
                        "Una opción seleccionada ya no está disponible para este producto."
                );
            }
            selected.add(option);
        }

        Map<Long, Long> selectedByGroup = selected.stream()
                .collect(Collectors.groupingBy(
                        ModifierOptionResponse::groupId,
                        Collectors.counting()
                ));
        for (ModifierGroupResponse group : groups) {
            long count = selectedByGroup.getOrDefault(group.id(), 0L);
            if (count < group.minSelections() || count > group.maxSelections()) {
                throw new BadRequestException(selectionMessage(group));
            }
        }
        return selected;
    }

    private String selectionMessage(ModifierGroupResponse group) {
        if (group.minSelections() == group.maxSelections()) {
            return "Selecciona " + group.minSelections() + " opción(es) en " + group.name() + ".";
        }
        return "Selecciona entre " + group.minSelections() + " y " + group.maxSelections()
                + " opción(es) en " + group.name() + ".";
    }

    private void saveModifiers(Long itemId, List<ModifierOptionResponse> options) {
        if (options.isEmpty()) {
            return;
        }
        modifierRepository.saveAll(
                options.stream().map(option -> new OrderItemModifier(itemId, option)).toList()
        );
        modifierRepository.flush();
    }

    private void publishItemsChanged(RestaurantOrder order) {
        realtimeEventPublisher.publish(
                RealtimeEventType.ORDER_ITEMS_CHANGED,
                order.getBranchId(),
                order.getId(),
                null,
                null
        );
    }

    OrderDetailResponse buildDetail(RestaurantOrder order) {
        List<OrderItem> items = repository.findByOrderIdOrderByCreatedAtAscIdAsc(order.getId());
        Map<Long, List<OrderItemModifier>> modifiersByItem = items.isEmpty()
                ? Map.of()
                : modifierRepository
                        .findByOrderItemIdInOrderByOrderItemIdAscIdAsc(
                                items.stream().map(OrderItem::getId).toList()
                        )
                        .stream()
                        .collect(Collectors.groupingBy(OrderItemModifier::getOrderItemId));

        List<OrderItemResponse> responses = items.stream()
                .map(item -> toResponse(
                        item,
                        modifiersByItem.getOrDefault(item.getId(), List.of())
                ))
                .toList();
        int totalUnits = responses.stream().mapToInt(OrderItemResponse::quantity).sum();
        BigDecimal productSubtotal = responses.stream()
                .map(OrderItemResponse::baseSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal modifierSubtotal = responses.stream()
                .map(OrderItemResponse::modifierSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new OrderDetailResponse(
                orderService.toResponse(order),
                responses,
                totalUnits,
                productSubtotal,
                modifierSubtotal,
                productSubtotal.add(modifierSubtotal)
        );
    }

    private OrderItemResponse toResponse(
            OrderItem item,
            List<OrderItemModifier> modifiers
    ) {
        BigDecimal quantity = BigDecimal.valueOf(item.getQuantity());
        BigDecimal baseSubtotal = item.getUnitPrice().multiply(quantity);
        BigDecimal modifierSubtotal = modifiers.stream()
                .map(OrderItemModifier::getPriceDelta)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .multiply(quantity);
        return new OrderItemResponse(
                item.getId(),
                item.getProductId(),
                item.getProductName(),
                item.getUnitPrice(),
                item.getDestination(),
                item.getQuantity(),
                item.getNotes(),
                modifiers.stream().map(OrderItemModifierResponse::from).toList(),
                baseSubtotal,
                modifierSubtotal,
                baseSubtotal.add(modifierSubtotal),
                item.getSentAt(),
                item.getVersion(),
                item.getCreatedAt(),
                item.getUpdatedAt()
        );
    }
}
