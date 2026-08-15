package com.rest.restaurantsystem.order;

import com.rest.restaurantsystem.catalog.product.ProductDestination;
import com.rest.restaurantsystem.catalog.product.ProductResponse;
import com.rest.restaurantsystem.exception.BadRequestException;
import com.rest.restaurantsystem.user.UserResponse;
import com.rest.restaurantsystem.user.UserRole;
import com.rest.restaurantsystem.user.UserService;
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
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PreparationServiceTest {

    @Mock
    private PreparationTicketRepository ticketRepository;

    @Mock
    private PreparationItemRepository preparationItemRepository;

    @Mock
    private PreparationItemModifierRepository preparationModifierRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private OrderItemModifierRepository orderModifierRepository;

    @Mock
    private RestaurantOrderRepository orderRepository;

    @Mock
    private OrderService orderService;

    @Mock
    private OrderItemService orderItemService;

    @Mock
    private UserService userService;

    @InjectMocks
    private PreparationService service;

    @Test
    void dispatchesSeparateTicketsForKitchenAndService() {
        RestaurantOrder order = order();
        OrderItem kitchenItem = item(90L, ProductDestination.PRODUCTION, "Hamburguesa");
        OrderItem serviceItem = item(91L, ProductDestination.SERVICE, "Refresco");
        AtomicLong ticketIds = new AtomicLong(100L);
        AtomicLong preparationItemIds = new AtomicLong(500L);
        List<PreparationItem> createdItems = new ArrayList<>();

        when(orderService.getAccessibleEntity(42L, "mesero")).thenReturn(order);
        when(orderItemRepository.findByOrderIdOrderByCreatedAtAscIdAsc(42L))
                .thenReturn(List.of(kitchenItem, serviceItem));
        when(orderModifierRepository.findByOrderItemIdInOrderByOrderItemIdAscIdAsc(anyList()))
                .thenReturn(List.of());
        when(userService.currentUser("mesero")).thenReturn(user());
        when(userService.findById(10L)).thenReturn(user());
        when(ticketRepository.saveAndFlush(any(PreparationTicket.class))).thenAnswer(invocation -> {
            PreparationTicket ticket = invocation.getArgument(0);
            ReflectionTestUtils.setField(ticket, "id", ticketIds.getAndIncrement());
            ticket.onCreate();
            return ticket;
        });
        when(preparationItemRepository.saveAndFlush(any(PreparationItem.class)))
                .thenAnswer(invocation -> {
                    PreparationItem item = invocation.getArgument(0);
                    ReflectionTestUtils.setField(item, "id", preparationItemIds.getAndIncrement());
                    item.onCreate();
                    createdItems.add(item);
                    return item;
                });
        when(preparationItemRepository.findByTicketIdInOrderByTicketIdAscCreatedAtAscIdAsc(anyList()))
                .thenAnswer(invocation -> List.copyOf(createdItems));
        when(preparationModifierRepository
                .findByPreparationItemIdInOrderByPreparationItemIdAscSortOrderAscIdAsc(anyList()))
                .thenReturn(List.of());
        when(orderRepository.findById(42L)).thenReturn(Optional.of(order));
        when(orderService.toResponse(order)).thenReturn(orderResponse(order));

        OrderDispatchResponse result = service.dispatch(
                42L,
                new PreparationDispatchRequest(0),
                "mesero"
        );

        assertThat(result.tickets()).hasSize(2);
        assertThat(result.tickets())
                .extracting(PreparationTicketResponse::destination)
                .containsExactly(ProductDestination.PRODUCTION, ProductDestination.SERVICE);
        assertThat(kitchenItem.getSentAt()).isNotNull();
        assertThat(serviceItem.getSentAt()).isNotNull();
    }

    @Test
    void rejectsSkippingFromPendingDirectlyToReady() {
        PreparationItem item = new PreparationItem(
                100L,
                item(90L, ProductDestination.PRODUCTION, "Hamburguesa")
        );
        ReflectionTestUtils.setField(item, "id", 500L);
        item.onCreate();
        PreparationTicket ticket = new PreparationTicket(
                42L,
                1L,
                ProductDestination.PRODUCTION,
                1,
                10L
        );
        ReflectionTestUtils.setField(ticket, "id", 100L);
        ticket.onCreate();
        when(preparationItemRepository.findById(500L)).thenReturn(Optional.of(item));
        when(ticketRepository.findById(100L)).thenReturn(Optional.of(ticket));

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> service.changeStatus(
                        500L,
                        new PreparationStatusRequest(PreparationStatus.READY, 0),
                        "cocinero"
                )
        );

        assertThat(exception.getMessage()).contains("No es posible cambiar");
    }

    private RestaurantOrder order() {
        RestaurantOrder order = new RestaurantOrder(
                new OrderCreateRequest(1L, 20L, ServiceMode.DINE_IN, 10L, 2, null, null),
                10L
        );
        ReflectionTestUtils.setField(order, "id", 42L);
        order.onCreate();
        return order;
    }

    private OrderItem item(Long id, ProductDestination destination, String name) {
        ProductResponse product = new ProductResponse(
                id,
                2L,
                "Categoría",
                "SKU-" + id,
                name,
                null,
                new BigDecimal("100.00"),
                destination,
                true,
                true,
                0,
                Instant.EPOCH,
                Instant.EPOCH
        );
        OrderItem item = new OrderItem(42L, product, 1, null);
        ReflectionTestUtils.setField(item, "id", id);
        item.onCreate();
        return item;
    }

    private UserResponse user() {
        return new UserResponse(
                10L,
                "mesero",
                "Mesero",
                UserRole.OPERATOR,
                true,
                false,
                0,
                Instant.EPOCH,
                Instant.EPOCH
        );
    }

    private OrderResponse orderResponse(RestaurantOrder order) {
        return new OrderResponse(
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
