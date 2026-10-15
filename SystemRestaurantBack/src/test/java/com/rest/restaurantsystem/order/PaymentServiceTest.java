package com.rest.restaurantsystem.order;

import com.rest.restaurantsystem.exception.BadRequestException;
import com.rest.restaurantsystem.cash.CashRegisterReference;
import com.rest.restaurantsystem.cash.CashRegisterService;
import com.rest.restaurantsystem.realtime.RealtimeEventPublisher;
import com.rest.restaurantsystem.realtime.RealtimeEventType;
import com.rest.restaurantsystem.user.UserResponse;
import com.rest.restaurantsystem.user.UserRole;
import com.rest.restaurantsystem.user.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private OrderPaymentRepository paymentRepository;

    @Mock
    private OrderDiscountRepository discountRepository;

    @Mock
    private RestaurantOrderRepository orderRepository;

    @Mock
    private OrderItemRepository itemRepository;

    @Mock
    private PreparationItemRepository preparationItemRepository;

    @Mock
    private OrderService orderService;

    @Mock
    private OrderItemService orderItemService;

    @Mock
    private UserService userService;

    @Mock
    private CashRegisterService cashRegisterService;

    @Mock
    private RealtimeEventPublisher realtimeEventPublisher;

    @InjectMocks
    private PaymentService service;

    private RestaurantOrder order;
    private UserResponse cashier;
    private OrderDetailResponse detail;

    @BeforeEach
    void setUp() {
        order = order();
        cashier = user();
        detail = detail("100.00");
    }

    @Test
    void registersCashPaymentWithTipWithoutIncreasingOrderBalance() {
        UUID operationId = UUID.randomUUID();
        List<OrderPayment> savedPayments = new ArrayList<>();
        mockAccessibleOrder();
        when(paymentRepository.findByOrderIdAndOperationId(42L, operationId))
                .thenReturn(Optional.empty());
        when(orderItemService.buildDetail(order)).thenReturn(detail);
        when(paymentRepository.sumActiveByOrderId(42L)).thenReturn(BigDecimal.ZERO);
        when(cashRegisterService.lockOpenForPayment(1L))
                .thenReturn(new CashRegisterReference(12L, "CAJ-000012"));
        when(paymentRepository.saveAndFlush(any(OrderPayment.class))).thenAnswer(invocation -> {
            OrderPayment payment = invocation.getArgument(0);
            ReflectionTestUtils.setField(payment, "id", 80L);
            payment.onCreate();
            savedPayments.add(payment);
            return payment;
        });
        when(orderRepository.saveAndFlush(order)).thenReturn(order);
        when(paymentRepository.findByOrderIdOrderByReceivedAtAscIdAsc(42L))
                .thenAnswer(invocation -> List.copyOf(savedPayments));
        when(userService.findById(10L)).thenReturn(cashier);

        OrderPaymentSummaryResponse result = service.add(
                42L,
                new PaymentCreateRequest(
                        operationId,
                        new BigDecimal("100.00"),
                        new BigDecimal("150.00"),
                        PaymentMethod.CASH,
                        null,
                        null,
                        null,
                        new BigDecimal("15.00"),
                        0
                ),
                "cajero"
        );

        assertThat(result.paid()).isEqualByComparingTo("100.00");
        assertThat(result.tipTotal()).isEqualByComparingTo("15.00");
        assertThat(result.balance()).isZero();
        assertThat(result.payments().getFirst().changeAmount()).isEqualByComparingTo("35.00");
        assertThat(result.payments().getFirst().tipBeneficiaryName()).isEqualTo("Caja principal");
        verify(realtimeEventPublisher).publish(
                RealtimeEventType.PAYMENT_CHANGED,
                1L,
                42L,
                null,
                null
        );
    }

    @Test
    void rejectsElectronicChargeThatDoesNotIncludeItsTip() {
        UUID operationId = UUID.randomUUID();
        mockAccessibleOrder();
        when(paymentRepository.findByOrderIdAndOperationId(42L, operationId))
                .thenReturn(Optional.empty());
        when(orderItemService.buildDetail(order)).thenReturn(detail);
        when(paymentRepository.sumActiveByOrderId(42L)).thenReturn(BigDecimal.ZERO);

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> service.add(42L, new PaymentCreateRequest(operationId,
                        new BigDecimal("100.00"), new BigDecimal("100.00"),
                        PaymentMethod.CARD, null, "AUT-123", null,
                        new BigDecimal("15.00"), 0), "cajero"));

        assertThat(exception.getMessage()).contains("consumo más propina");
        verify(paymentRepository, never()).saveAndFlush(any());
    }

    @Test
    void voidingPaymentReversesConsumptionAndTipTogether() {
        OrderPayment payment = new OrderPayment(UUID.randomUUID(), 42L, 12L,
                new BigDecimal("100.00"), new BigDecimal("115.00"), PaymentMethod.CARD,
                null, "AUT-123", null, 10L,
                new BigDecimal("15.00"), 10L, "Caja principal");
        ReflectionTestUtils.setField(payment, "id", 80L);
        payment.onCreate();
        mockAccessibleOrder();
        when(paymentRepository.findById(80L)).thenReturn(Optional.of(payment));
        when(paymentRepository.saveAndFlush(payment)).thenReturn(payment);
        when(orderRepository.saveAndFlush(order)).thenReturn(order);
        when(orderItemService.buildDetail(order)).thenReturn(detail);
        when(paymentRepository.findByOrderIdOrderByReceivedAtAscIdAsc(42L))
                .thenReturn(List.of(payment));
        when(userService.findById(10L)).thenReturn(cashier);

        OrderPaymentSummaryResponse result = service.voidPayment(42L, 80L,
                new PaymentVoidRequest(0, "Cobro duplicado"), "cajero");

        assertThat(result.paid()).isZero();
        assertThat(result.tipTotal()).isZero();
        assertThat(result.balance()).isEqualByComparingTo("100.00");
        assertThat(result.payments().getFirst().status()).isEqualTo(PaymentStatus.VOIDED);
    }

    @Test
    void rejectsPaymentGreaterThanOutstandingBalance() {
        UUID operationId = UUID.randomUUID();
        mockAccessibleOrder();
        when(paymentRepository.findByOrderIdAndOperationId(42L, operationId))
                .thenReturn(Optional.empty());
        when(orderItemService.buildDetail(order)).thenReturn(detail);
        when(paymentRepository.sumActiveByOrderId(42L)).thenReturn(new BigDecimal("80.00"));

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> service.add(
                        42L,
                        new PaymentCreateRequest(
                                operationId,
                                new BigDecimal("30.00"),
                                new BigDecimal("30.00"),
                                PaymentMethod.CARD,
                                null,
                                null,
                                null,
                                0
                        ),
                        "cajero"
                )
        );

        assertThat(exception.getMessage()).contains("$20.00");
    }

    @Test
    void rejectsGrossPaymentWhenDiscountReducedOutstandingBalance() {
        UUID operationId = UUID.randomUUID();
        OrderDiscount discount = new OrderDiscount(UUID.randomUUID(), 42L, null,
                OrderDiscountSource.MANUAL, null, "Descuento autorizado",
                com.rest.restaurantsystem.promotion.DiscountType.FIXED,
                new BigDecimal("15.00"), new BigDecimal("100.00"),
                new BigDecimal("15.00"), "Atención por demora", 10L);
        mockAccessibleOrder();
        when(paymentRepository.findByOrderIdAndOperationId(42L, operationId))
                .thenReturn(Optional.empty());
        when(orderItemService.buildDetail(order)).thenReturn(detail);
        when(paymentRepository.sumActiveByOrderId(42L)).thenReturn(BigDecimal.ZERO);
        when(discountRepository.findByOrderIdAndStatus(42L, OrderDiscountStatus.ACTIVE))
                .thenReturn(Optional.of(discount));

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> service.add(42L, new PaymentCreateRequest(operationId,
                        new BigDecimal("100.00"), new BigDecimal("100.00"),
                        PaymentMethod.CASH, null, null, null, 0), "cajero"));

        assertThat(exception.getMessage()).contains("$85.00");
        verify(paymentRepository, never()).saveAndFlush(any());
    }

    @Test
    void closesOnlyASettledOrderWithoutActivePreparation() {
        OrderPayment payment = new OrderPayment(
                UUID.randomUUID(),
                42L,
                12L,
                new BigDecimal("100.00"),
                new BigDecimal("100.00"),
                PaymentMethod.CARD,
                null,
                "AUT-123",
                null,
                10L
        );
        ReflectionTestUtils.setField(payment, "id", 80L);
        payment.onCreate();
        mockAccessibleOrder();
        when(orderItemService.buildDetail(order)).thenReturn(detail);
        when(paymentRepository.findByOrderIdOrderByReceivedAtAscIdAsc(42L))
                .thenReturn(List.of(payment));
        when(userService.findById(10L)).thenReturn(cashier);
        when(orderRepository.saveAndFlush(order)).thenReturn(order);

        service.close(
                42L,
                new PaymentCloseRequest(0),
                "cajero"
        );

        assertThat(order.getStatus()).isEqualTo(OrderStatus.COMPLETED);
        verify(realtimeEventPublisher).publish(
                RealtimeEventType.PAYMENT_CHANGED,
                1L,
                42L,
                null,
                null
        );
    }

    private void mockAccessibleOrder() {
        when(orderRepository.findLockedById(42L)).thenReturn(Optional.of(order));
        when(userService.currentUser("cajero")).thenReturn(cashier);
    }

    private RestaurantOrder order() {
        RestaurantOrder entity = new RestaurantOrder(
                new OrderCreateRequest(1L, 20L, ServiceMode.DINE_IN, 10L, 2, null, null),
                10L
        );
        ReflectionTestUtils.setField(entity, "id", 42L);
        entity.onCreate();
        entity.changeStatus(OrderStatus.IN_PROGRESS);
        return entity;
    }

    private UserResponse user() {
        return new UserResponse(
                10L,
                "cajero",
                "Caja principal",
                UserRole.CASHIER,
                true,
                false,
                0,
                Instant.EPOCH,
                Instant.EPOCH
        );
    }

    private OrderDetailResponse detail(String total) {
        OrderResponse response = new OrderResponse(
                42L,
                "PED-000042",
                1L,
                "Sucursal principal",
                20L,
                "Mesa 1",
                5L,
                "Comedor",
                ServiceMode.DINE_IN,
                10L,
                "Caja principal",
                10L,
                "Caja principal",
                2,
                null,
                null,
                OrderStatus.IN_PROGRESS,
                0,
                Instant.EPOCH,
                Instant.EPOCH,
                null
        );
        return new OrderDetailResponse(
                response,
                List.of(org.mockito.Mockito.mock(OrderItemResponse.class)),
                1,
                new BigDecimal(total),
                BigDecimal.ZERO,
                new BigDecimal(total)
        );
    }
}
