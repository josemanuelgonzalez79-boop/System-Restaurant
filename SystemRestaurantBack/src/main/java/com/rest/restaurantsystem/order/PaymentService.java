package com.rest.restaurantsystem.order;

import com.rest.restaurantsystem.catalog.product.ProductDestination;
import com.rest.restaurantsystem.cash.CashRegisterReference;
import com.rest.restaurantsystem.cash.CashRegisterService;
import com.rest.restaurantsystem.exception.BadRequestException;
import com.rest.restaurantsystem.exception.ConflictException;
import com.rest.restaurantsystem.exception.ResourceNotFoundException;
import com.rest.restaurantsystem.realtime.RealtimeEventPublisher;
import com.rest.restaurantsystem.realtime.RealtimeEventType;
import com.rest.restaurantsystem.user.UserResponse;
import com.rest.restaurantsystem.user.UserRole;
import com.rest.restaurantsystem.user.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@Service
public class PaymentService {

    private static final Set<OrderStatus> ACTIVE_ORDER_STATUSES =
            EnumSet.of(OrderStatus.OPEN, OrderStatus.IN_PROGRESS);
    private static final Set<ProductDestination> ROUTED_DESTINATIONS =
            EnumSet.of(ProductDestination.PRODUCTION, ProductDestination.SERVICE);
    private static final Set<UserRole> VOID_ROLES =
            EnumSet.of(UserRole.OWNER, UserRole.ADMIN, UserRole.MANAGER, UserRole.CASHIER);

    private final OrderPaymentRepository paymentRepository;
    private final RestaurantOrderRepository orderRepository;
    private final OrderItemRepository itemRepository;
    private final PreparationItemRepository preparationItemRepository;
    private final OrderService orderService;
    private final OrderItemService orderItemService;
    private final UserService userService;
    private final CashRegisterService cashRegisterService;
    private final RealtimeEventPublisher realtimeEventPublisher;

    public PaymentService(
            OrderPaymentRepository paymentRepository,
            RestaurantOrderRepository orderRepository,
            OrderItemRepository itemRepository,
            PreparationItemRepository preparationItemRepository,
            OrderService orderService,
            OrderItemService orderItemService,
            UserService userService,
            CashRegisterService cashRegisterService,
            RealtimeEventPublisher realtimeEventPublisher
    ) {
        this.paymentRepository = paymentRepository;
        this.orderRepository = orderRepository;
        this.itemRepository = itemRepository;
        this.preparationItemRepository = preparationItemRepository;
        this.orderService = orderService;
        this.orderItemService = orderItemService;
        this.userService = userService;
        this.cashRegisterService = cashRegisterService;
        this.realtimeEventPublisher = realtimeEventPublisher;
    }

    @Transactional(readOnly = true)
    public OrderPaymentSummaryResponse findSummary(Long orderId, String currentUsername) {
        RestaurantOrder order = orderService.getAccessibleEntity(orderId, currentUsername);
        return buildSummary(order);
    }

    @Transactional
    public OrderPaymentSummaryResponse add(
            Long orderId,
            PaymentCreateRequest request,
            String currentUsername
    ) {
        RestaurantOrder order = lockedOrder(orderId);
        UserResponse currentUser = accessibleUser(order, currentUsername);

        if (paymentRepository.findByOrderIdAndOperationId(orderId, request.operationId()).isPresent()) {
            return buildSummary(order);
        }
        ensureActive(order);
        ensureVersion(order, request.orderVersion());

        OrderDetailResponse detail = orderItemService.buildDetail(order);
        if (detail.items().isEmpty() || detail.total().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Agrega productos al pedido antes de registrar un cobro.");
        }
        BigDecimal paid = paymentRepository.sumActiveByOrderId(orderId);
        BigDecimal balance = detail.total().subtract(paid);
        if (balance.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("El pedido ya está totalmente pagado.");
        }
        if (request.amount().compareTo(balance) > 0) {
            throw new BadRequestException(
                    "El importe aplicado no puede superar el saldo pendiente de $"
                            + balance.toPlainString() + "."
            );
        }

        BigDecimal tenderedAmount = validateTenderedAmount(request);
        String methodLabel = validateMethodLabel(request);
        CashRegisterReference cashRegister = cashRegisterService.lockOpenForPayment(
                order.getBranchId()
        );
        paymentRepository.saveAndFlush(
                new OrderPayment(
                        request.operationId(),
                        orderId,
                        cashRegister.id(),
                        request.amount(),
                        tenderedAmount,
                        request.method(),
                        methodLabel,
                        request.reference(),
                        request.notes(),
                        currentUser.id()
                )
        );
        order.touch();
        orderRepository.saveAndFlush(order);

        OrderPaymentSummaryResponse response = buildSummary(order);
        publishChanged(order);
        return response;
    }

    @Transactional
    public OrderPaymentSummaryResponse voidPayment(
            Long orderId,
            Long paymentId,
            PaymentVoidRequest request,
            String currentUsername
    ) {
        RestaurantOrder order = lockedOrder(orderId);
        UserResponse currentUser = accessibleUser(order, currentUsername);
        ensureVoidPermission(currentUser);
        ensureActive(order);
        ensureVersion(order, request.orderVersion());

        OrderPayment payment = paymentRepository.findById(paymentId)
                .filter(candidate -> candidate.getOrderId().equals(orderId))
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró el cobro solicitado."));
        if (payment.getStatus() == PaymentStatus.VOIDED) {
            throw new BadRequestException("El cobro ya fue anulado.");
        }
        cashRegisterService.ensurePaymentSessionOpen(payment.getCashRegisterSessionId());
        payment.voidPayment(currentUser.id(), request.reason());
        paymentRepository.saveAndFlush(payment);
        order.touch();
        orderRepository.saveAndFlush(order);

        OrderPaymentSummaryResponse response = buildSummary(order);
        publishChanged(order);
        return response;
    }

    @Transactional
    public OrderPaymentSummaryResponse close(
            Long orderId,
            PaymentCloseRequest request,
            String currentUsername
    ) {
        RestaurantOrder order = lockedOrder(orderId);
        accessibleUser(order, currentUsername);
        ensureActive(order);
        ensureVersion(order, request.orderVersion());

        OrderPaymentSummaryResponse current = buildSummary(order);
        if (!current.canClose()) {
            throw new BadRequestException(current.closeBlockingReason());
        }
        order.changeStatus(OrderStatus.COMPLETED);
        orderRepository.saveAndFlush(order);

        OrderPaymentSummaryResponse response = buildSummary(order);
        publishChanged(order);
        return response;
    }

    private RestaurantOrder lockedOrder(Long orderId) {
        return orderRepository.findLockedById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró el pedido solicitado."));
    }

    private UserResponse accessibleUser(RestaurantOrder order, String username) {
        orderService.ensureAccessibleBranch(order.getBranchId(), username);
        UserResponse user = userService.currentUser(username);
        if (!user.active()) {
            throw new BadRequestException("Tu usuario está inactivo.");
        }
        return user;
    }

    private void ensureActive(RestaurantOrder order) {
        if (!ACTIVE_ORDER_STATUSES.contains(order.getStatus())) {
            throw new BadRequestException("El pedido ya está cerrado y no admite cambios de cobro.");
        }
    }

    private void ensureVersion(RestaurantOrder order, long requestedVersion) {
        if (order.getVersion() != requestedVersion) {
            throw new ConflictException(
                    "El pedido cambió en otra caja o tablet. Actualiza antes de continuar."
            );
        }
    }

    private BigDecimal validateTenderedAmount(PaymentCreateRequest request) {
        BigDecimal tendered = request.tenderedAmount() == null
                ? request.amount()
                : request.tenderedAmount();
        if (request.method() == PaymentMethod.CASH) {
            if (tendered.compareTo(request.amount()) < 0) {
                throw new BadRequestException(
                        "El efectivo recibido debe cubrir el importe que se aplicará."
                );
            }
            return tendered;
        }
        if (tendered.compareTo(request.amount()) != 0) {
            throw new BadRequestException(
                    "En pagos distintos de efectivo, el importe recibido debe coincidir con el aplicado."
            );
        }
        return request.amount();
    }

    private String validateMethodLabel(PaymentCreateRequest request) {
        String label = request.methodLabel() == null ? null : request.methodLabel().trim();
        if (request.method() == PaymentMethod.OTHER) {
            if (label == null || label.isBlank()) {
                throw new BadRequestException("Escribe el nombre de la otra forma de pago.");
            }
            return label;
        }
        return null;
    }

    private void ensureVoidPermission(UserResponse user) {
        if (!VOID_ROLES.contains(user.role())) {
            throw new BadRequestException(
                    "Solo propietario, administrador, gerente o cajero puede anular cobros."
            );
        }
    }

    private OrderPaymentSummaryResponse buildSummary(RestaurantOrder order) {
        OrderDetailResponse detail = orderItemService.buildDetail(order);
        List<OrderPayment> entities = paymentRepository
                .findByOrderIdOrderByReceivedAtAscIdAsc(order.getId());
        BigDecimal paid = entities.stream()
                .filter(payment -> payment.getStatus() == PaymentStatus.ACTIVE)
                .map(OrderPayment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal balance = detail.total().subtract(paid).max(BigDecimal.ZERO);
        boolean settled = detail.total().compareTo(BigDecimal.ZERO) > 0
                && balance.compareTo(BigDecimal.ZERO) == 0;
        String blockingReason = closeBlockingReason(order, detail, settled, balance);
        CashRegisterReference openCashRegister = cashRegisterService
                .findOpenReference(order.getBranchId())
                .orElse(null);

        return new OrderPaymentSummaryResponse(
                detail,
                entities.stream().map(this::toResponse).toList(),
                detail.total(),
                paid,
                balance,
                settled,
                blockingReason == null,
                blockingReason,
                openCashRegister == null ? null : openCashRegister.id(),
                openCashRegister == null ? null : openCashRegister.folio()
        );
    }

    private String closeBlockingReason(
            RestaurantOrder order,
            OrderDetailResponse detail,
            boolean settled,
            BigDecimal balance
    ) {
        if (!ACTIVE_ORDER_STATUSES.contains(order.getStatus())) {
            return "El pedido ya está cerrado.";
        }
        if (detail.items().isEmpty()) {
            return "Agrega al menos un producto antes de cerrar el pedido.";
        }
        if (!settled) {
            return "Falta cobrar $" + balance.toPlainString() + ".";
        }
        if (itemRepository.existsByOrderIdAndSentAtIsNullAndDestinationIn(
                order.getId(),
                ROUTED_DESTINATIONS
        )) {
            return "Envía todas las partidas de cocina o servicio antes de cerrar el pedido.";
        }
        if (preparationItemRepository.existsActiveByOrderId(order.getId())) {
            return "Aún hay partidas pendientes, en preparación o listas por entregar.";
        }
        return null;
    }

    private PaymentResponse toResponse(OrderPayment payment) {
        UserResponse receivedBy = userService.findById(payment.getReceivedByUserId());
        UserResponse voidedBy = payment.getVoidedByUserId() == null
                ? null
                : userService.findById(payment.getVoidedByUserId());
        return new PaymentResponse(
                payment.getId(),
                "PAG-%06d".formatted(payment.getId()),
                payment.getOperationId(),
                payment.getCashRegisterSessionId(),
                payment.getAmount(),
                payment.getTenderedAmount(),
                payment.getChangeAmount(),
                payment.getMethod(),
                payment.getMethodLabel(),
                payment.getReference(),
                payment.getNotes(),
                payment.getStatus(),
                payment.getReceivedByUserId(),
                receivedBy.fullName(),
                payment.getReceivedAt(),
                payment.getVoidedByUserId(),
                voidedBy == null ? null : voidedBy.fullName(),
                payment.getVoidedAt(),
                payment.getVoidReason()
        );
    }

    private void publishChanged(RestaurantOrder order) {
        realtimeEventPublisher.publish(
                RealtimeEventType.PAYMENT_CHANGED,
                order.getBranchId(),
                order.getId(),
                null,
                null
        );
    }
}
