package com.rest.restaurantsystem.order;

import com.rest.restaurantsystem.customer.CustomerResponse;
import com.rest.restaurantsystem.customer.CustomerService;
import com.rest.restaurantsystem.exception.BadRequestException;
import com.rest.restaurantsystem.exception.ConflictException;
import com.rest.restaurantsystem.realtime.RealtimeEventPublisher;
import com.rest.restaurantsystem.realtime.RealtimeEventType;
import com.rest.restaurantsystem.reservation.ReservationService;
import com.rest.restaurantsystem.structure.BranchAssignmentService;
import com.rest.restaurantsystem.structure.BranchResponse;
import com.rest.restaurantsystem.structure.BranchService;
import com.rest.restaurantsystem.structure.ServicePointResponse;
import com.rest.restaurantsystem.structure.ServicePointService;
import com.rest.restaurantsystem.structure.ServicePointType;
import com.rest.restaurantsystem.user.UserResponse;
import com.rest.restaurantsystem.user.UserRole;
import com.rest.restaurantsystem.user.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private RestaurantOrderRepository repository;

    @Mock
    private PreparationItemRepository preparationItemRepository;

    @Mock
    private OrderPaymentRepository paymentRepository;

    @Mock
    private BranchService branchService;

    @Mock
    private ServicePointService servicePointService;

    @Mock
    private UserService userService;

    @Mock
    private BranchAssignmentService assignmentService;

    @Mock
    private RealtimeEventPublisher realtimeEventPublisher;

    @Mock
    private ReservationService reservationService;

    @Mock
    private CustomerService customerService;

    @InjectMocks
    private OrderService service;

    @Test
    void opensDineInOrderForAvailablePoint() {
        OrderCreateRequest request = dineInRequest();
        mockValidUsersAndBranch();
        when(servicePointService.findById(20L)).thenReturn(point(20L, 1L));
        when(repository.existsByServicePointIdAndStatusIn(any(), any())).thenReturn(false);
        when(repository.saveAndFlush(any(RestaurantOrder.class))).thenAnswer(invocation -> {
            RestaurantOrder order = invocation.getArgument(0);
            ReflectionTestUtils.setField(order, "id", 42L);
            order.onCreate();
            return order;
        });

        OrderResponse result = service.create(request, "mesero");

        assertThat(result.folio()).isEqualTo("PED-000042");
        assertThat(result.servicePointName()).isEqualTo("Mesa 1");
        assertThat(result.status()).isEqualTo(OrderStatus.OPEN);
        verify(realtimeEventPublisher).publish(
                RealtimeEventType.ORDER_CREATED,
                1L,
                42L,
                null,
                null
        );
    }

    @Test
    void openingFromReservationLinksTheCreatedOrderInOneTransaction() {
        OrderCreateRequest request = new OrderCreateRequest(1L, 20L, ServiceMode.DINE_IN,
                10L, 2, "Ana", null, 7L);
        mockValidUsersAndBranch(UserRole.CASHIER);
        when(reservationService.beforeOrder(7L, 1L, 20L)).thenReturn(50L);
        when(servicePointService.findById(20L)).thenReturn(point(20L, 1L));
        when(repository.saveAndFlush(any(RestaurantOrder.class))).thenAnswer(invocation -> {
            RestaurantOrder order = invocation.getArgument(0);
            ReflectionTestUtils.setField(order, "id", 42L);
            order.onCreate();
            return order;
        });

        OrderResponse result = service.create(request, "mesero");

        assertThat(result.folio()).isEqualTo("PED-000042");
        verify(repository).saveAndFlush(org.mockito.ArgumentMatchers.argThat(
                (RestaurantOrder order) -> order.getCustomerId().equals(50L)));
        verify(reservationService).beforeOrder(7L, 1L, 20L);
        verify(reservationService).afterOrder(7L, 42L, "mesero");
    }

    @Test
    void walkInWithCustomerCopiesNameAndStoresLink() {
        OrderCreateRequest request = new OrderCreateRequest(1L, 20L, ServiceMode.DINE_IN,
                10L, 2, null, null, null, 50L);
        mockValidUsersAndBranch(UserRole.CASHIER);
        when(servicePointService.findById(20L)).thenReturn(point(20L, 1L));
        when(customerService.requireActive(50L)).thenReturn(new CustomerResponse(50L, "Ana",
                "6671234567", null, null, null, true, 0L, "caja", "caja",
                Instant.EPOCH, Instant.EPOCH));
        when(repository.saveAndFlush(any(RestaurantOrder.class))).thenAnswer(invocation -> {
            RestaurantOrder order = invocation.getArgument(0);
            ReflectionTestUtils.setField(order, "id", 42L);
            order.onCreate();
            return order;
        });

        OrderResponse response = service.create(request, "mesero");

        assertThat(response.customerReference()).isEqualTo("Ana");
        verify(repository).saveAndFlush(org.mockito.ArgumentMatchers.argThat(
                (RestaurantOrder order) -> order.getCustomerId().equals(50L)));
    }

    @Test
    void operatorCannotAttachCustomerProfile() {
        mockValidUsersAndBranch();
        when(servicePointService.findById(20L)).thenReturn(point(20L, 1L));
        OrderCreateRequest request = new OrderCreateRequest(1L, 20L, ServiceMode.DINE_IN,
                10L, 2, null, null, null, 50L);

        assertThrows(BadRequestException.class, () -> service.create(request, "mesero"));
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void rejectsPointFromAnotherBranch() {
        mockValidUsersAndBranch();
        when(servicePointService.findById(20L)).thenReturn(point(20L, 2L));

        assertThrows(BadRequestException.class, () -> service.create(dineInRequest(), "mesero"));
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void rejectsPointWithActiveOrder() {
        mockValidUsersAndBranch();
        when(servicePointService.findById(20L)).thenReturn(point(20L, 1L));
        when(repository.existsByServicePointIdAndStatusIn(any(), any())).thenReturn(true);

        assertThrows(ConflictException.class, () -> service.create(dineInRequest(), "mesero"));
    }

    @Test
    void movesOpenOrderToInProgress() {
        RestaurantOrder order = persistedOrder();
        when(repository.findLockedById(42L)).thenReturn(Optional.of(order));
        when(userService.currentUser("mesero")).thenReturn(user(10L, "mesero", "Mesero"));
        when(assignmentService.isAssigned(1L, 10L)).thenReturn(true);
        when(branchService.findById(1L)).thenReturn(branch(1L));
        when(servicePointService.findById(20L)).thenReturn(point(20L, 1L));
        when(userService.findById(10L)).thenReturn(user(10L, "mesero", "Mesero"));
        when(repository.saveAndFlush(order)).thenReturn(order);

        OrderResponse result = service.changeStatus(
                42L,
                new OrderStatusRequest(OrderStatus.IN_PROGRESS, 0),
                "mesero"
        );

        assertThat(result.status()).isEqualTo(OrderStatus.IN_PROGRESS);
        assertThat(result.closedAt()).isNull();
    }

    @Test
    void doesNotReopenCompletedOrder() {
        RestaurantOrder order = persistedOrder();
        order.changeStatus(OrderStatus.COMPLETED);
        when(repository.findLockedById(42L)).thenReturn(Optional.of(order));
        when(userService.currentUser("mesero")).thenReturn(user(10L, "mesero", "Mesero"));
        when(assignmentService.isAssigned(1L, 10L)).thenReturn(true);

        assertThrows(
                BadRequestException.class,
                () -> service.changeStatus(
                        42L,
                        new OrderStatusRequest(OrderStatus.IN_PROGRESS, 0),
                        "mesero"
                )
        );
    }

    @Test
    void rejectsDirectCompletionBecausePaymentFlowOwnsTheClose() {
        RestaurantOrder order = persistedOrder();
        order.changeStatus(OrderStatus.IN_PROGRESS);
        when(repository.findLockedById(42L)).thenReturn(Optional.of(order));
        when(userService.currentUser("mesero")).thenReturn(user(10L, "mesero", "Mesero"));
        when(assignmentService.isAssigned(1L, 10L)).thenReturn(true);

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> service.changeStatus(
                        42L,
                        new OrderStatusRequest(OrderStatus.COMPLETED, 0),
                        "mesero"
                )
        );

        assertThat(exception.getMessage()).contains("Cobro");
    }

    @Test
    void rejectsCancellingWhilePreparationIsActive() {
        RestaurantOrder order = persistedOrder();
        order.changeStatus(OrderStatus.IN_PROGRESS);
        when(repository.findLockedById(42L)).thenReturn(Optional.of(order));
        when(userService.currentUser("mesero")).thenReturn(user(10L, "mesero", "Mesero"));
        when(assignmentService.isAssigned(1L, 10L)).thenReturn(true);
        when(preparationItemRepository.existsActiveByOrderId(42L)).thenReturn(true);

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> service.changeStatus(
                        42L,
                        new OrderStatusRequest(OrderStatus.CANCELLED, 0),
                        "mesero"
                )
        );

        assertThat(exception.getMessage()).contains("Cancela primero");
    }

    @Test
    void rejectsCancellingWhileAnActivePaymentExists() {
        RestaurantOrder order = persistedOrder();
        order.changeStatus(OrderStatus.IN_PROGRESS);
        when(repository.findLockedById(42L)).thenReturn(Optional.of(order));
        when(userService.currentUser("mesero")).thenReturn(user(10L, "mesero", "Mesero"));
        when(assignmentService.isAssigned(1L, 10L)).thenReturn(true);
        when(paymentRepository.existsByOrderIdAndStatus(42L, PaymentStatus.ACTIVE))
                .thenReturn(true);

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> service.changeStatus(
                        42L,
                        new OrderStatusRequest(OrderStatus.CANCELLED, 0),
                        "mesero"
                )
        );

        assertThat(exception.getMessage()).contains("Anula primero");
    }

    private void mockValidUsersAndBranch() {
        mockValidUsersAndBranch(UserRole.OPERATOR);
    }

    private void mockValidUsersAndBranch(UserRole role) {
        when(branchService.findById(1L)).thenReturn(branch(1L));
        UserResponse person = user(10L, "mesero", "Mesero");
        when(userService.currentUser("mesero")).thenReturn(new UserResponse(person.id(),
                person.username(), person.fullName(), role, person.active(), person.mustChangePassword(),
                person.version(), person.createdAt(), person.updatedAt()));
        when(userService.findById(10L)).thenReturn(user(10L, "mesero", "Mesero"));
        when(assignmentService.isAssigned(1L, 10L)).thenReturn(true);
    }

    private RestaurantOrder persistedOrder() {
        RestaurantOrder order = new RestaurantOrder(dineInRequest(), 10L);
        ReflectionTestUtils.setField(order, "id", 42L);
        order.onCreate();
        return order;
    }

    private OrderCreateRequest dineInRequest() {
        return new OrderCreateRequest(
                1L,
                20L,
                ServiceMode.DINE_IN,
                10L,
                2,
                null,
                "Sin cebolla"
        );
    }

    private BranchResponse branch(Long id) {
        return new BranchResponse(
                id,
                "PRINCIPAL",
                "Sucursal principal",
                null,
                null,
                "America/Mazatlan",
                0,
                true,
                0,
                Instant.EPOCH,
                Instant.EPOCH
        );
    }

    private ServicePointResponse point(Long id, Long branchId) {
        return new ServicePointResponse(
                id,
                5L,
                "Comedor",
                branchId,
                "Sucursal principal",
                "MESA-01",
                "Mesa 1",
                null,
                ServicePointType.TABLE,
                0,
                true,
                0,
                Instant.EPOCH,
                Instant.EPOCH
        );
    }

    private UserResponse user(Long id, String username, String fullName) {
        return new UserResponse(
                id,
                username,
                fullName,
                UserRole.OPERATOR,
                true,
                false,
                0,
                Instant.EPOCH,
                Instant.EPOCH
        );
    }
}
