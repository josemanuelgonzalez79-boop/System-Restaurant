package com.rest.restaurantsystem.order;

import com.rest.restaurantsystem.exception.BadRequestException;
import com.rest.restaurantsystem.exception.ConflictException;
import com.rest.restaurantsystem.promotion.DiscountType;
import com.rest.restaurantsystem.promotion.PromotionResponse;
import com.rest.restaurantsystem.promotion.PromotionService;
import com.rest.restaurantsystem.realtime.RealtimeEventPublisher;
import com.rest.restaurantsystem.user.UserResponse;
import com.rest.restaurantsystem.user.UserRole;
import com.rest.restaurantsystem.user.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderDiscountServiceTest {
    @Mock OrderDiscountRepository discounts;
    @Mock RestaurantOrderRepository orders;
    @Mock OrderPaymentRepository payments;
    @Mock OrderService orderService;
    @Mock OrderItemService items;
    @Mock PaymentService paymentService;
    @Mock PromotionService promotions;
    @Mock UserService users;
    @Mock RealtimeEventPublisher realtime;
    @InjectMocks OrderDiscountService service;

    @Test
    void freezesPromotionNameAndAmountOnTheOrder() {
        RestaurantOrder order = mock(RestaurantOrder.class);
        OrderDetailResponse detail = mock(OrderDetailResponse.class);
        PromotionResponse offer = new PromotionResponse(7L, "MARTES10", "Martes",
                DiscountType.PERCENT, new BigDecimal("10.00"), BigDecimal.ZERO,
                java.time.LocalDate.now(), java.time.LocalDate.now(), true, 0,
                "gerente", Instant.EPOCH);
        when(orderService.getLockedAccessibleEntity(42L, "gerente")).thenReturn(order);
        when(users.currentUser("gerente")).thenReturn(user(UserRole.MANAGER));
        when(order.getStatus()).thenReturn(OrderStatus.OPEN);
        when(order.getVersion()).thenReturn(2L);
        when(items.buildDetail(order)).thenReturn(detail);
        when(detail.total()).thenReturn(new BigDecimal("200.00"));
        when(detail.items()).thenReturn(List.of(mock(OrderItemResponse.class)));
        when(promotions.requireEligible(7L, new BigDecimal("200.00"))).thenReturn(offer);
        when(promotions.calculate(DiscountType.PERCENT, new BigDecimal("10.00"),
                new BigDecimal("200.00"))).thenReturn(new BigDecimal("20.00"));

        service.apply(42L, new DiscountApplyRequest(UUID.randomUUID(), 7L,
                null, null, null, 2L), "gerente");

        ArgumentCaptor<OrderDiscount> saved = ArgumentCaptor.forClass(OrderDiscount.class);
        verify(discounts).saveAndFlush(saved.capture());
        assertThat(saved.getValue().getCodeSnapshot()).isEqualTo("MARTES10");
        assertThat(saved.getValue().getNameSnapshot()).isEqualTo("Martes");
        assertThat(saved.getValue().getAmount()).isEqualByComparingTo("20.00");
        verify(orders).saveAndFlush(order);
    }

    @Test
    void cashierCannotGrantManualDiscountEvenWithDirectApiCall() {
        RestaurantOrder order = mock(RestaurantOrder.class);
        OrderDetailResponse detail = mock(OrderDetailResponse.class);
        when(orderService.getLockedAccessibleEntity(42L, "cajero")).thenReturn(order);
        when(users.currentUser("cajero")).thenReturn(user(UserRole.CASHIER));
        when(order.getStatus()).thenReturn(OrderStatus.OPEN);
        when(order.getVersion()).thenReturn(2L);
        when(items.buildDetail(order)).thenReturn(detail);
        when(detail.total()).thenReturn(new BigDecimal("100.00"));
        when(detail.items()).thenReturn(List.of(mock(OrderItemResponse.class)));

        assertThrows(BadRequestException.class, () -> service.apply(42L,
                new DiscountApplyRequest(UUID.randomUUID(), null, DiscountType.PERCENT,
                        new BigDecimal("10.00"), "Atención por demora", 2L), "cajero"));

        verify(discounts, never()).saveAndFlush(any());
    }

    @Test
    void rejectsStaleTabletVersionBeforeWritingDiscount() {
        RestaurantOrder order = mock(RestaurantOrder.class);
        when(orderService.getLockedAccessibleEntity(42L, "gerente")).thenReturn(order);
        when(users.currentUser("gerente")).thenReturn(user(UserRole.MANAGER));
        when(order.getStatus()).thenReturn(OrderStatus.OPEN);
        when(order.getVersion()).thenReturn(3L);

        assertThrows(ConflictException.class, () -> service.apply(42L,
                new DiscountApplyRequest(UUID.randomUUID(), 7L, null, null, null, 2L),
                "gerente"));

        verify(discounts, never()).saveAndFlush(any());
    }

    private UserResponse user(UserRole role) {
        return new UserResponse(10L, "operador", "Operador", role, true, false,
                0, Instant.EPOCH, Instant.EPOCH);
    }
}
