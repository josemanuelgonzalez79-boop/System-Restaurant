package com.rest.restaurantsystem.order;

import com.rest.restaurantsystem.exception.BadRequestException;
import com.rest.restaurantsystem.exception.ConflictException;
import com.rest.restaurantsystem.exception.ResourceNotFoundException;
import com.rest.restaurantsystem.promotion.PromotionResponse;
import com.rest.restaurantsystem.promotion.PromotionService;
import com.rest.restaurantsystem.realtime.RealtimeEventPublisher;
import com.rest.restaurantsystem.realtime.RealtimeEventType;
import com.rest.restaurantsystem.user.UserResponse;
import com.rest.restaurantsystem.user.UserRole;
import com.rest.restaurantsystem.user.UserService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.EnumSet;
import java.util.Set;

@Service
public class OrderDiscountService {
    private static final Set<UserRole> APPLY_ROLES =
            EnumSet.of(UserRole.OWNER, UserRole.ADMIN, UserRole.MANAGER, UserRole.CASHIER);
    private static final Set<UserRole> MANUAL_ROLES =
            EnumSet.of(UserRole.OWNER, UserRole.ADMIN, UserRole.MANAGER);

    private final OrderDiscountRepository discounts;
    private final RestaurantOrderRepository orders;
    private final OrderPaymentRepository payments;
    private final OrderService orderService;
    private final OrderItemService items;
    private final PaymentService paymentService;
    private final PromotionService promotions;
    private final UserService users;
    private final RealtimeEventPublisher realtime;

    public OrderDiscountService(OrderDiscountRepository discounts, RestaurantOrderRepository orders,
                                OrderPaymentRepository payments, OrderService orderService,
                                OrderItemService items, PaymentService paymentService,
                                PromotionService promotions, UserService users,
                                RealtimeEventPublisher realtime) {
        this.discounts = discounts;
        this.orders = orders;
        this.payments = payments;
        this.orderService = orderService;
        this.items = items;
        this.paymentService = paymentService;
        this.promotions = promotions;
        this.users = users;
        this.realtime = realtime;
    }

    @Transactional
    public OrderPaymentSummaryResponse apply(Long orderId, DiscountApplyRequest request,
                                             String username) {
        RestaurantOrder order = orderService.getLockedAccessibleEntity(orderId, username);
        UserResponse user = authorized(username);
        if (discounts.findByOrderIdAndOperationId(orderId, request.operationId()).isPresent()) {
            return paymentService.findSummary(orderId, username);
        }
        ensureEditable(order, request.orderVersion());
        if (discounts.existsByOrderIdAndStatus(orderId, OrderDiscountStatus.ACTIVE)) {
            throw new BadRequestException("Anula el descuento activo antes de aplicar otro.");
        }
        if (payments.existsByOrderIdAndStatus(orderId, PaymentStatus.ACTIVE)) {
            throw new BadRequestException("Anula los cobros activos antes de cambiar el descuento.");
        }
        OrderDetailResponse detail = items.buildDetail(order);
        BigDecimal subtotal = detail.total();
        if (detail.items().isEmpty() || subtotal.signum() <= 0) {
            throw new BadRequestException("Agrega productos antes de aplicar un descuento.");
        }

        OrderDiscount discount;
        if (request.promotionId() != null) {
            if (request.manualType() != null || request.manualValue() != null
                    || request.reason() != null && !request.reason().isBlank()) {
                throw new BadRequestException("Selecciona una promoción o un descuento manual, no ambos.");
            }
            PromotionResponse offer = promotions.requireEligible(request.promotionId(), subtotal);
            BigDecimal amount = promotions.calculate(offer.discountType(), offer.discountValue(), subtotal);
            discount = new OrderDiscount(request.operationId(), orderId, offer.id(),
                    OrderDiscountSource.PROMOTION, offer.code(), offer.name(), offer.discountType(),
                    offer.discountValue(), subtotal, amount, null, user.id());
        } else {
            if (!MANUAL_ROLES.contains(user.role())) {
                throw new BadRequestException("Solo propietario, administrador o gerente puede dar un descuento manual.");
            }
            if (request.manualType() == null || request.manualValue() == null
                    || request.reason() == null || request.reason().trim().length() < 5) {
                throw new BadRequestException("Indica tipo, valor y motivo de al menos 5 caracteres.");
            }
            BigDecimal amount = promotions.calculate(request.manualType(), request.manualValue(), subtotal);
            discount = new OrderDiscount(request.operationId(), orderId, null,
                    OrderDiscountSource.MANUAL, null, "Descuento autorizado", request.manualType(),
                    request.manualValue(), subtotal, amount, request.reason().trim(), user.id());
        }
        try {
            discounts.saveAndFlush(discount);
        } catch (DataIntegrityViolationException exception) {
            throw new ConflictException("El descuento cambió en otra caja. Actualiza la cuenta.");
        }
        order.touch();
        orders.saveAndFlush(order);
        realtime.publish(RealtimeEventType.PAYMENT_CHANGED, order.getBranchId(), orderId, null, null);
        return paymentService.findSummary(orderId, username);
    }

    @Transactional
    public OrderPaymentSummaryResponse voidDiscount(Long orderId, Long discountId,
                                                    DiscountVoidRequest request, String username) {
        RestaurantOrder order = orderService.getLockedAccessibleEntity(orderId, username);
        UserResponse user = authorized(username);
        ensureEditable(order, request.orderVersion());
        if (request.reason() == null || request.reason().trim().length() < 5) {
            throw new BadRequestException("Escribe un motivo de anulación de al menos 5 caracteres.");
        }
        OrderDiscount discount = discounts.findById(discountId)
                .filter(item -> item.getOrderId().equals(orderId))
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró el descuento."));
        if (discount.getStatus() != OrderDiscountStatus.ACTIVE) {
            throw new BadRequestException("El descuento ya fue anulado.");
        }
        if (payments.existsByOrderIdAndStatus(orderId, PaymentStatus.ACTIVE)) {
            throw new BadRequestException("Anula los cobros activos antes de quitar el descuento.");
        }
        discount.voidDiscount(user.id(), request.reason());
        discounts.saveAndFlush(discount);
        order.touch();
        orders.saveAndFlush(order);
        realtime.publish(RealtimeEventType.PAYMENT_CHANGED, order.getBranchId(), orderId, null, null);
        return paymentService.findSummary(orderId, username);
    }

    private UserResponse authorized(String username) {
        UserResponse user = users.currentUser(username);
        if (!user.active() || !APPLY_ROLES.contains(user.role())) {
            throw new BadRequestException("Tu usuario no puede aplicar ni anular descuentos.");
        }
        return user;
    }

    private void ensureEditable(RestaurantOrder order, long version) {
        if (order.getStatus() != OrderStatus.OPEN && order.getStatus() != OrderStatus.IN_PROGRESS) {
            throw new BadRequestException("El pedido está cerrado y no admite descuentos.");
        }
        if (order.getVersion() != version) {
            throw new ConflictException("La cuenta cambió en otra tablet. Actualiza antes de continuar.");
        }
    }
}
