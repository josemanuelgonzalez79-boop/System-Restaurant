package com.rest.restaurantsystem.order;

import com.rest.restaurantsystem.catalog.modifier.ModifierGroupResponse;
import com.rest.restaurantsystem.catalog.modifier.ModifierGroupService;
import com.rest.restaurantsystem.catalog.modifier.ModifierOptionResponse;
import com.rest.restaurantsystem.catalog.product.ProductDestination;
import com.rest.restaurantsystem.catalog.product.ProductResponse;
import com.rest.restaurantsystem.catalog.product.ProductService;
import com.rest.restaurantsystem.exception.BadRequestException;
import com.rest.restaurantsystem.realtime.RealtimeEventPublisher;
import com.rest.restaurantsystem.realtime.RealtimeEventType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderItemServiceTest {

    @Mock
    private OrderItemRepository repository;

    @Mock
    private OrderItemModifierRepository modifierRepository;

    @Mock
    private RestaurantOrderRepository orderRepository;

    @Mock
    private OrderPaymentRepository paymentRepository;

    @Mock
    private OrderService orderService;

    @Mock
    private ProductService productService;

    @Mock
    private ModifierGroupService modifierGroupService;

    @Mock
    private RealtimeEventPublisher realtimeEventPublisher;

    @InjectMocks
    private OrderItemService service;

    @Test
    void addsProductWithModifierAndCalculatesTotalInBackend() {
        RestaurantOrder order = order();
        ProductResponse product = product();
        ModifierOptionResponse option = option();
        AtomicReference<OrderItem> savedItem = new AtomicReference<>();
        OrderItemModifier snapshotModifier = new OrderItemModifier(90L, option);
        ReflectionTestUtils.setField(snapshotModifier, "id", 33L);

        when(orderService.getLockedAccessibleEntity(42L, "mesero")).thenReturn(order);
        when(productService.findOrderableById(8L)).thenReturn(product);
        when(modifierGroupService.findAll(8L)).thenReturn(List.of(group(option)));
        when(repository.saveAndFlush(any(OrderItem.class))).thenAnswer(invocation -> {
            OrderItem item = invocation.getArgument(0);
            ReflectionTestUtils.setField(item, "id", 90L);
            item.onCreate();
            savedItem.set(item);
            return item;
        });
        when(orderRepository.saveAndFlush(order)).thenReturn(order);
        when(repository.findByOrderIdOrderByCreatedAtAscIdAsc(42L))
                .thenAnswer(invocation -> List.of(savedItem.get()));
        when(modifierRepository.findByOrderItemIdInOrderByOrderItemIdAscIdAsc(anyList()))
                .thenReturn(List.of(snapshotModifier));
        when(orderService.toResponse(order)).thenAnswer(invocation -> response(order));

        OrderDetailResponse result = service.add(
                42L,
                new OrderItemCreateRequest(8L, 2, "Sin cebolla", List.of(15L), 0),
                "mesero"
        );

        assertThat(result.totalUnits()).isEqualTo(2);
        assertThat(result.productSubtotal()).isEqualByComparingTo("200.00");
        assertThat(result.modifierSubtotal()).isEqualByComparingTo("50.00");
        assertThat(result.total()).isEqualByComparingTo("250.00");
        assertThat(result.order().status()).isEqualTo(OrderStatus.IN_PROGRESS);
        verify(realtimeEventPublisher).publish(
                RealtimeEventType.ORDER_ITEMS_CHANGED,
                1L,
                42L,
                null,
                null
        );
    }

    @Test
    void rejectsProductWhenRequiredModifierIsMissing() {
        RestaurantOrder order = order();
        when(orderService.getLockedAccessibleEntity(42L, "mesero")).thenReturn(order);
        when(productService.findOrderableById(8L)).thenReturn(product());
        when(modifierGroupService.findAll(8L)).thenReturn(List.of(group(option())));

        assertThrows(
                BadRequestException.class,
                () -> service.add(
                        42L,
                        new OrderItemCreateRequest(8L, 1, null, List.of(), 0),
                        "mesero"
                )
        );

        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void rejectsEditingAnItemAlreadySentToPreparation() {
        RestaurantOrder order = order();
        OrderItem item = new OrderItem(42L, product(), 1, null);
        ReflectionTestUtils.setField(item, "id", 90L);
        item.onCreate();
        item.markSent(Instant.now());
        when(orderService.getLockedAccessibleEntity(42L, "mesero")).thenReturn(order);
        when(repository.findById(90L)).thenReturn(java.util.Optional.of(item));

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> service.update(
                        42L,
                        90L,
                        new OrderItemUpdateRequest(2, null, List.of(), 0),
                        "mesero"
                )
        );

        assertThat(exception.getMessage()).contains("ya fue enviada");
        verify(repository, never()).saveAndFlush(item);
    }

    @Test
    void rejectsAddingProductsAfterTheFirstPayment() {
        RestaurantOrder order = order();
        when(orderService.getLockedAccessibleEntity(42L, "mesero")).thenReturn(order);
        when(paymentRepository.existsByOrderIdAndStatus(42L, PaymentStatus.ACTIVE))
                .thenReturn(true);

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> service.add(
                        42L,
                        new OrderItemCreateRequest(8L, 1, null, List.of(15L), 0),
                        "mesero"
                )
        );

        assertThat(exception.getMessage()).contains("cobros registrados");
        verify(repository, never()).saveAndFlush(any());
    }

    private RestaurantOrder order() {
        RestaurantOrder order = new RestaurantOrder(
                new OrderCreateRequest(
                        1L,
                        20L,
                        ServiceMode.DINE_IN,
                        10L,
                        2,
                        null,
                        null
                ),
                10L
        );
        ReflectionTestUtils.setField(order, "id", 42L);
        order.onCreate();
        return order;
    }

    private ProductResponse product() {
        return new ProductResponse(
                8L,
                2L,
                "Hamburguesas",
                "HAM-01",
                "Hamburguesa clásica",
                null,
                new BigDecimal("100.00"),
                ProductDestination.PRODUCTION,
                true,
                true,
                0,
                Instant.EPOCH,
                Instant.EPOCH
        );
    }

    private ModifierOptionResponse option() {
        return new ModifierOptionResponse(
                15L,
                5L,
                "Tamaño",
                8L,
                "Grande",
                new BigDecimal("25.00"),
                0,
                true,
                0,
                Instant.EPOCH,
                Instant.EPOCH
        );
    }

    private ModifierGroupResponse group(ModifierOptionResponse option) {
        return new ModifierGroupResponse(
                5L,
                8L,
                "Hamburguesa clásica",
                "Tamaño",
                1,
                1,
                0,
                true,
                0,
                Instant.EPOCH,
                Instant.EPOCH,
                List.of(option)
        );
    }

    private OrderResponse response(RestaurantOrder order) {
        return new OrderResponse(
                order.getId(),
                "PED-000042",
                1L,
                "Sucursal principal",
                20L,
                "Mesa 1",
                5L,
                "Comedor",
                ServiceMode.DINE_IN,
                10L,
                "Mesero",
                10L,
                "Mesero",
                2,
                null,
                null,
                order.getStatus(),
                order.getVersion(),
                order.getOpenedAt(),
                order.getUpdatedAt(),
                order.getClosedAt()
        );
    }
}
