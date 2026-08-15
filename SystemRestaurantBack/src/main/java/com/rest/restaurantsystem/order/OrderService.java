package com.rest.restaurantsystem.order;

import com.rest.restaurantsystem.catalog.product.ProductDestination;
import com.rest.restaurantsystem.exception.BadRequestException;
import com.rest.restaurantsystem.exception.ConflictException;
import com.rest.restaurantsystem.exception.ResourceNotFoundException;
import com.rest.restaurantsystem.realtime.RealtimeEventPublisher;
import com.rest.restaurantsystem.realtime.RealtimeEventType;
import com.rest.restaurantsystem.structure.BranchAssignmentService;
import com.rest.restaurantsystem.structure.BranchResponse;
import com.rest.restaurantsystem.structure.BranchService;
import com.rest.restaurantsystem.structure.ServicePointResponse;
import com.rest.restaurantsystem.structure.ServicePointService;
import com.rest.restaurantsystem.user.UserResponse;
import com.rest.restaurantsystem.user.UserService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@Service
public class OrderService {

    private static final Set<OrderStatus> ACTIVE_STATUSES =
            EnumSet.of(OrderStatus.OPEN, OrderStatus.IN_PROGRESS);
    private static final Set<ProductDestination> ROUTED_DESTINATIONS =
            EnumSet.of(ProductDestination.PRODUCTION, ProductDestination.SERVICE);

    private final RestaurantOrderRepository repository;
    private final OrderItemRepository itemRepository;
    private final PreparationItemRepository preparationItemRepository;
    private final BranchService branchService;
    private final ServicePointService servicePointService;
    private final UserService userService;
    private final BranchAssignmentService assignmentService;
    private final RealtimeEventPublisher realtimeEventPublisher;

    public OrderService(
            RestaurantOrderRepository repository,
            OrderItemRepository itemRepository,
            PreparationItemRepository preparationItemRepository,
            BranchService branchService,
            ServicePointService servicePointService,
            UserService userService,
            BranchAssignmentService assignmentService,
            RealtimeEventPublisher realtimeEventPublisher
    ) {
        this.repository = repository;
        this.itemRepository = itemRepository;
        this.preparationItemRepository = preparationItemRepository;
        this.branchService = branchService;
        this.servicePointService = servicePointService;
        this.userService = userService;
        this.assignmentService = assignmentService;
        this.realtimeEventPublisher = realtimeEventPublisher;
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> findAll(Long branchId, boolean activeOnly, String currentUsername) {
        UserResponse currentUser = userService.currentUser(currentUsername);
        Set<Long> allowedBranchIds = assignmentService.findAssignedBranchIds(currentUser.id());
        if (branchId != null && !allowedBranchIds.contains(branchId)) {
            throw new BadRequestException("Tu usuario no está asignado a la sucursal seleccionada.");
        }

        List<RestaurantOrder> orders;
        if (branchId == null && activeOnly) {
            orders = repository.findAllByStatusInOrderByOpenedAtDesc(ACTIVE_STATUSES);
        } else if (branchId == null) {
            orders = repository.findAllByOrderByOpenedAtDesc();
        } else if (activeOnly) {
            orders = repository.findAllByBranchIdAndStatusInOrderByOpenedAtDesc(
                    branchId,
                    ACTIVE_STATUSES
            );
        } else {
            orders = repository.findAllByBranchIdOrderByOpenedAtDesc(branchId);
        }

        return orders.stream()
                .filter(order -> allowedBranchIds.contains(order.getBranchId()))
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public OrderResponse findById(Long id, String currentUsername) {
        RestaurantOrder order = getEntity(id);
        ensureCurrentUserAssigned(order.getBranchId(), currentUsername);
        return toResponse(order);
    }

    @Transactional(readOnly = true)
    public List<BranchResponse> findBranches(String currentUsername) {
        UserResponse currentUser = userService.currentUser(currentUsername);
        Set<Long> allowedBranchIds = assignmentService.findAssignedBranchIds(currentUser.id());
        return branchService.findAll().stream()
                .filter(BranchResponse::active)
                .filter(branch -> allowedBranchIds.contains(branch.id()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<OrderOperatorResponse> findOperators(Long branchId, String currentUsername) {
        ensureCurrentUserAssigned(branchId, currentUsername);
        return assignmentService.findAssignedActiveUsers(branchId).stream()
                .map(OrderOperatorResponse::from)
                .toList();
    }

    @Transactional
    public OrderResponse create(OrderCreateRequest request, String currentUsername) {
        BranchResponse branch = branchService.findById(request.branchId());
        if (!branch.active()) {
            throw new BadRequestException("La sucursal debe estar activa para abrir pedidos.");
        }

        UserResponse currentUser = ensureCurrentUserAssigned(request.branchId(), currentUsername);
        validateAssignedUser(request.branchId(), request.assignedUserId());
        validateServicePoint(request);

        RestaurantOrder order = new RestaurantOrder(request, currentUser.id());
        try {
            RestaurantOrder saved = repository.saveAndFlush(order);
            OrderResponse response = toResponse(saved);
            realtimeEventPublisher.publish(
                    RealtimeEventType.ORDER_CREATED,
                    saved.getBranchId(),
                    saved.getId(),
                    null,
                    null
            );
            return response;
        } catch (DataIntegrityViolationException exception) {
            throw new ConflictException("El punto seleccionado ya tiene un pedido abierto.");
        }
    }

    @Transactional
    public OrderResponse changeStatus(
            Long id,
            OrderStatusRequest request,
            String currentUsername
    ) {
        RestaurantOrder order = getEntity(id);
        ensureCurrentUserAssigned(order.getBranchId(), currentUsername);
        if (order.getVersion() != request.version()) {
            throw new ConflictException(
                    "El pedido cambió en otro dispositivo. Actualiza la pantalla e intenta nuevamente."
            );
        }
        if (order.getStatus() == request.status()) {
            return toResponse(order);
        }
        validateTransition(order.getStatus(), request.status());
        if (request.status() == OrderStatus.COMPLETED
                && itemRepository.countByOrderId(order.getId()) == 0) {
            throw new BadRequestException("Agrega al menos un producto antes de completar el pedido.");
        }
        if (request.status() == OrderStatus.COMPLETED
                && itemRepository.existsByOrderIdAndSentAtIsNullAndDestinationIn(
                        order.getId(),
                        ROUTED_DESTINATIONS
                )) {
            throw new BadRequestException(
                    "Envía todas las partidas de cocina o servicio antes de completar el pedido."
            );
        }
        if (request.status() == OrderStatus.COMPLETED
                && preparationItemRepository.existsActiveByOrderId(order.getId())) {
            throw new BadRequestException(
                    "Aún hay partidas pendientes, en preparación o listas por entregar."
            );
        }
        if (request.status() == OrderStatus.CANCELLED
                && preparationItemRepository.existsActiveByOrderId(order.getId())) {
            throw new BadRequestException(
                    "Cancela primero las partidas activas desde la pantalla de Preparación."
            );
        }
        order.changeStatus(request.status());
        RestaurantOrder saved = repository.saveAndFlush(order);
        OrderResponse response = toResponse(saved);
        realtimeEventPublisher.publish(
                RealtimeEventType.ORDER_UPDATED,
                saved.getBranchId(),
                saved.getId(),
                null,
                null
        );
        return response;
    }

    private void validateServicePoint(OrderCreateRequest request) {
        if (request.serviceMode() == ServiceMode.TAKEOUT) {
            if (request.servicePointId() != null) {
                throw new BadRequestException("Un pedido para llevar no debe ocupar una mesa o punto.");
            }
            return;
        }

        if (request.servicePointId() == null) {
            throw new BadRequestException("Selecciona una mesa o punto para el pedido en el local.");
        }

        ServicePointResponse point = servicePointService.findById(request.servicePointId());
        if (!point.active()) {
            throw new BadRequestException("La mesa o punto seleccionado está inactivo.");
        }
        if (!point.branchId().equals(request.branchId())) {
            throw new BadRequestException("La mesa o punto no pertenece a la sucursal seleccionada.");
        }
        if (repository.existsByServicePointIdAndStatusIn(point.id(), ACTIVE_STATUSES)) {
            throw new ConflictException("La mesa o punto ya tiene un pedido abierto.");
        }
    }

    private UserResponse validateAssignedUser(Long branchId, Long userId) {
        UserResponse user = userService.findById(userId);
        if (!user.active()) {
            throw new BadRequestException("El responsable seleccionado está inactivo.");
        }
        if (!assignmentService.isAssigned(branchId, userId)) {
            throw new BadRequestException("El responsable no está asignado a la sucursal.");
        }
        return user;
    }

    private UserResponse ensureCurrentUserAssigned(Long branchId, String username) {
        UserResponse currentUser = userService.currentUser(username);
        if (!assignmentService.isAssigned(branchId, currentUser.id())) {
            throw new BadRequestException("Tu usuario no está asignado a la sucursal seleccionada.");
        }
        return currentUser;
    }

    private void validateTransition(OrderStatus current, OrderStatus requested) {
        boolean allowed = switch (current) {
            case OPEN -> requested == OrderStatus.IN_PROGRESS || requested == OrderStatus.CANCELLED;
            case IN_PROGRESS -> requested == OrderStatus.COMPLETED
                    || requested == OrderStatus.CANCELLED;
            case COMPLETED, CANCELLED -> false;
        };
        if (!allowed) {
            throw new BadRequestException(
                    "No es posible cambiar el pedido de " + current + " a " + requested + "."
            );
        }
    }

    RestaurantOrder getAccessibleEntity(Long id, String currentUsername) {
        RestaurantOrder order = getEntity(id);
        ensureCurrentUserAssigned(order.getBranchId(), currentUsername);
        return order;
    }

    void ensureAccessibleBranch(Long branchId, String currentUsername) {
        ensureCurrentUserAssigned(branchId, currentUsername);
    }

    private RestaurantOrder getEntity(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró el pedido solicitado."));
    }

    OrderResponse toResponse(RestaurantOrder order) {
        BranchResponse branch = branchService.findById(order.getBranchId());
        ServicePointResponse point = order.getServicePointId() == null
                ? null
                : servicePointService.findById(order.getServicePointId());
        UserResponse assignedUser = userService.findById(order.getAssignedUserId());
        UserResponse openedByUser = userService.findById(order.getOpenedByUserId());

        return new OrderResponse(
                order.getId(),
                "PED-%06d".formatted(order.getId()),
                order.getBranchId(),
                branch.name(),
                order.getServicePointId(),
                point == null ? null : point.name(),
                point == null ? null : point.areaId(),
                point == null ? null : point.areaName(),
                order.getServiceMode(),
                order.getAssignedUserId(),
                assignedUser.fullName(),
                order.getOpenedByUserId(),
                openedByUser.fullName(),
                order.getGuestCount(),
                order.getCustomerReference(),
                order.getNotes(),
                order.getStatus(),
                order.getVersion(),
                order.getOpenedAt(),
                order.getUpdatedAt(),
                order.getClosedAt()
        );
    }
}
