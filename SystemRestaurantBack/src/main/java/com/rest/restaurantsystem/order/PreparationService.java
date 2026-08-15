package com.rest.restaurantsystem.order;

import com.rest.restaurantsystem.catalog.product.ProductDestination;
import com.rest.restaurantsystem.exception.BadRequestException;
import com.rest.restaurantsystem.exception.ConflictException;
import com.rest.restaurantsystem.exception.ResourceNotFoundException;
import com.rest.restaurantsystem.realtime.RealtimeEventPublisher;
import com.rest.restaurantsystem.realtime.RealtimeEventType;
import com.rest.restaurantsystem.user.UserResponse;
import com.rest.restaurantsystem.user.UserService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class PreparationService {

    private static final Set<ProductDestination> ROUTED_DESTINATIONS =
            EnumSet.of(ProductDestination.PRODUCTION, ProductDestination.SERVICE);
    private static final Set<PreparationStatus> ACTIVE_STATUSES =
            EnumSet.of(
                    PreparationStatus.PENDING,
                    PreparationStatus.IN_PREPARATION,
                    PreparationStatus.READY
            );

    private final PreparationTicketRepository ticketRepository;
    private final PreparationItemRepository preparationItemRepository;
    private final PreparationItemModifierRepository preparationModifierRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderItemModifierRepository orderModifierRepository;
    private final RestaurantOrderRepository orderRepository;
    private final OrderService orderService;
    private final OrderItemService orderItemService;
    private final UserService userService;
    private final RealtimeEventPublisher realtimeEventPublisher;

    public PreparationService(
            PreparationTicketRepository ticketRepository,
            PreparationItemRepository preparationItemRepository,
            PreparationItemModifierRepository preparationModifierRepository,
            OrderItemRepository orderItemRepository,
            OrderItemModifierRepository orderModifierRepository,
            RestaurantOrderRepository orderRepository,
            OrderService orderService,
            OrderItemService orderItemService,
            UserService userService,
            RealtimeEventPublisher realtimeEventPublisher
    ) {
        this.ticketRepository = ticketRepository;
        this.preparationItemRepository = preparationItemRepository;
        this.preparationModifierRepository = preparationModifierRepository;
        this.orderItemRepository = orderItemRepository;
        this.orderModifierRepository = orderModifierRepository;
        this.orderRepository = orderRepository;
        this.orderService = orderService;
        this.orderItemService = orderItemService;
        this.userService = userService;
        this.realtimeEventPublisher = realtimeEventPublisher;
    }

    @Transactional
    public OrderDispatchResponse dispatch(
            Long orderId,
            PreparationDispatchRequest request,
            String currentUsername
    ) {
        RestaurantOrder order = orderService.getAccessibleEntity(orderId, currentUsername);
        ensureEditable(order);
        if (order.getVersion() != request.orderVersion()) {
            throw new ConflictException(
                    "El pedido cambió en otro dispositivo. Actualiza la pantalla e intenta nuevamente."
            );
        }

        List<OrderItem> pendingItems = orderItemRepository
                .findByOrderIdOrderByCreatedAtAscIdAsc(orderId)
                .stream()
                .filter(item -> item.getSentAt() == null)
                .filter(item -> ROUTED_DESTINATIONS.contains(item.getDestination()))
                .toList();
        if (pendingItems.isEmpty()) {
            throw new BadRequestException(
                    "No hay partidas nuevas con ruta de cocina o servicio para enviar."
            );
        }

        Map<Long, List<OrderItemModifier>> modifiersByOrderItem = orderModifierRepository
                .findByOrderItemIdInOrderByOrderItemIdAscIdAsc(
                        pendingItems.stream().map(OrderItem::getId).toList()
                )
                .stream()
                .collect(Collectors.groupingBy(
                        OrderItemModifier::getOrderItemId,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));
        UserResponse currentUser = userService.currentUser(currentUsername);
        List<PreparationTicket> createdTickets = new ArrayList<>();

        try {
            for (ProductDestination destination : List.of(
                    ProductDestination.PRODUCTION,
                    ProductDestination.SERVICE
            )) {
                List<OrderItem> destinationItems = pendingItems.stream()
                        .filter(item -> item.getDestination() == destination)
                        .toList();
                if (destinationItems.isEmpty()) {
                    continue;
                }
                int sequence = Math.toIntExact(
                        ticketRepository.countByOrderIdAndDestination(orderId, destination) + 1
                );
                PreparationTicket ticket = ticketRepository.saveAndFlush(
                        new PreparationTicket(
                                orderId,
                                order.getBranchId(),
                                destination,
                                sequence,
                                currentUser.id()
                        )
                );
                createdTickets.add(ticket);

                for (OrderItem orderItem : destinationItems) {
                    PreparationItem preparationItem = preparationItemRepository.saveAndFlush(
                            new PreparationItem(ticket.getId(), orderItem)
                    );
                    List<OrderItemModifier> modifiers = modifiersByOrderItem.getOrDefault(
                            orderItem.getId(),
                            List.of()
                    );
                    if (!modifiers.isEmpty()) {
                        List<PreparationItemModifier> snapshots = new ArrayList<>();
                        for (int index = 0; index < modifiers.size(); index++) {
                            snapshots.add(new PreparationItemModifier(
                                    preparationItem.getId(),
                                    modifiers.get(index),
                                    index
                            ));
                        }
                        preparationModifierRepository.saveAll(snapshots);
                    }
                    orderItem.markSent(ticket.getSentAt());
                }
            }
        } catch (DataIntegrityViolationException | OptimisticLockingFailureException exception) {
            throw new ConflictException(
                    "Otra tablet ya envió estas partidas. Actualiza el pedido antes de continuar."
            );
        }

        orderItemRepository.saveAll(pendingItems);
        order.touch();
        try {
            orderRepository.saveAndFlush(order);
        } catch (OptimisticLockingFailureException exception) {
            throw new ConflictException(
                    "Otra tablet modificó el pedido durante el envío. Actualiza e intenta nuevamente."
            );
        }
        OrderDispatchResponse response = new OrderDispatchResponse(
                orderItemService.buildDetail(order),
                buildResponses(createdTickets)
        );
        realtimeEventPublisher.publish(
                RealtimeEventType.PREPARATION_DISPATCHED,
                order.getBranchId(),
                order.getId(),
                null,
                null
        );
        return response;
    }

    @Transactional(readOnly = true)
    public List<PreparationTicketResponse> findAll(
            Long branchId,
            ProductDestination destination,
            boolean activeOnly,
            String currentUsername
    ) {
        if (branchId == null) {
            throw new BadRequestException("Selecciona una sucursal para consultar preparación.");
        }
        if (destination == ProductDestination.NONE) {
            throw new BadRequestException("La ruta SIN PREPARACIÓN no genera comandas.");
        }
        orderService.ensureAccessibleBranch(branchId, currentUsername);
        List<PreparationTicket> tickets = destination == null
                ? ticketRepository.findByBranchIdOrderBySentAtAscIdAsc(branchId)
                : ticketRepository.findByBranchIdAndDestinationOrderBySentAtAscIdAsc(
                        branchId,
                        destination
                );
        List<PreparationTicketResponse> responses = buildResponses(tickets);
        if (!activeOnly) {
            return responses;
        }
        return responses.stream()
                .filter(ticket -> ticket.items().stream()
                        .anyMatch(item -> ACTIVE_STATUSES.contains(item.status())))
                .toList();
    }

    @Transactional
    public PreparationTicketResponse changeStatus(
            Long itemId,
            PreparationStatusRequest request,
            String currentUsername
    ) {
        PreparationItem item = preparationItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontró la partida de preparación."
                ));
        PreparationTicket ticket = ticketRepository.findById(item.getTicketId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontró la comanda de preparación."
                ));
        orderService.ensureAccessibleBranch(ticket.getBranchId(), currentUsername);
        if (item.getVersion() != request.version()) {
            throw new ConflictException(
                    "La partida cambió en otra pantalla. Actualiza e intenta nuevamente."
            );
        }
        if (item.getStatus() == request.status()) {
            return buildResponses(List.of(ticket)).getFirst();
        }
        validateTransition(item.getStatus(), request.status());
        item.changeStatus(request.status());
        try {
            preparationItemRepository.saveAndFlush(item);
        } catch (OptimisticLockingFailureException exception) {
            throw new ConflictException(
                    "La partida cambió en otra pantalla. Actualiza e intenta nuevamente."
            );
        }
        PreparationTicketResponse response = buildResponses(List.of(ticket)).getFirst();
        realtimeEventPublisher.publish(
                RealtimeEventType.PREPARATION_ITEM_CHANGED,
                ticket.getBranchId(),
                ticket.getOrderId(),
                ticket.getId(),
                item.getId()
        );
        return response;
    }

    private void ensureEditable(RestaurantOrder order) {
        if (order.getStatus() != OrderStatus.OPEN && order.getStatus() != OrderStatus.IN_PROGRESS) {
            throw new BadRequestException("El pedido está cerrado y ya no admite comandas.");
        }
    }

    private void validateTransition(PreparationStatus current, PreparationStatus requested) {
        boolean allowed = switch (current) {
            case PENDING -> requested == PreparationStatus.IN_PREPARATION
                    || requested == PreparationStatus.CANCELLED;
            case IN_PREPARATION -> requested == PreparationStatus.READY
                    || requested == PreparationStatus.CANCELLED;
            case READY -> requested == PreparationStatus.DELIVERED
                    || requested == PreparationStatus.CANCELLED;
            case DELIVERED, CANCELLED -> false;
        };
        if (!allowed) {
            throw new BadRequestException(
                    "No es posible cambiar la partida de " + current + " a " + requested + "."
            );
        }
    }

    private List<PreparationTicketResponse> buildResponses(List<PreparationTicket> tickets) {
        if (tickets.isEmpty()) {
            return List.of();
        }
        List<PreparationItem> items = preparationItemRepository
                .findByTicketIdInOrderByTicketIdAscCreatedAtAscIdAsc(
                        tickets.stream().map(PreparationTicket::getId).toList()
                );
        Map<Long, List<PreparationItem>> itemsByTicket = items.stream()
                .collect(Collectors.groupingBy(
                        PreparationItem::getTicketId,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));
        Map<Long, List<PreparationItemModifier>> modifiersByItem = items.isEmpty()
                ? Map.of()
                : preparationModifierRepository
                        .findByPreparationItemIdInOrderByPreparationItemIdAscSortOrderAscIdAsc(
                                items.stream().map(PreparationItem::getId).toList()
                        )
                        .stream()
                        .collect(Collectors.groupingBy(
                                PreparationItemModifier::getPreparationItemId,
                                LinkedHashMap::new,
                                Collectors.toList()
                        ));
        Map<Long, OrderResponse> orders = tickets.stream()
                .map(PreparationTicket::getOrderId)
                .distinct()
                .map(orderRepository::findById)
                .flatMap(java.util.Optional::stream)
                .map(orderService::toResponse)
                .collect(Collectors.toMap(
                        OrderResponse::id,
                        Function.identity(),
                        (left, right) -> left
                ));

        return tickets.stream().map(ticket -> {
            List<PreparationItemResponse> itemResponses = itemsByTicket
                    .getOrDefault(ticket.getId(), List.of())
                    .stream()
                    .map(item -> new PreparationItemResponse(
                            item.getId(),
                            item.getOrderItemId(),
                            item.getProductName(),
                            item.getQuantity(),
                            item.getNotes(),
                            item.getStatus(),
                            modifiersByItem.getOrDefault(item.getId(), List.of())
                                    .stream()
                                    .map(PreparationModifierResponse::from)
                                    .toList(),
                            item.getVersion(),
                            item.getStartedAt(),
                            item.getReadyAt(),
                            item.getDeliveredAt(),
                            item.getCancelledAt(),
                            item.getCreatedAt(),
                            item.getUpdatedAt()
                    ))
                    .toList();
            UserResponse sentBy = userService.findById(ticket.getSentByUserId());
            return new PreparationTicketResponse(
                    ticket.getId(),
                    orders.get(ticket.getOrderId()),
                    ticket.getDestination(),
                    ticket.getSequenceNumber(),
                    ticket.getSentByUserId(),
                    sentBy.fullName(),
                    ticket.getSentAt(),
                    aggregateStatus(itemResponses),
                    itemResponses
            );
        }).toList();
    }

    private PreparationStatus aggregateStatus(List<PreparationItemResponse> items) {
        if (items.stream().anyMatch(item -> item.status() == PreparationStatus.IN_PREPARATION)) {
            return PreparationStatus.IN_PREPARATION;
        }
        if (items.stream().anyMatch(item -> item.status() == PreparationStatus.PENDING)) {
            return PreparationStatus.PENDING;
        }
        if (items.stream().anyMatch(item -> item.status() == PreparationStatus.READY)) {
            return PreparationStatus.READY;
        }
        if (items.stream().anyMatch(item -> item.status() == PreparationStatus.DELIVERED)) {
            return PreparationStatus.DELIVERED;
        }
        return PreparationStatus.CANCELLED;
    }
}
