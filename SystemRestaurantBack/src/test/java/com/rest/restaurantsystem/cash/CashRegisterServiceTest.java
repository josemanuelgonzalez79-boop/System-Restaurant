package com.rest.restaurantsystem.cash;

import com.rest.restaurantsystem.exception.BadRequestException;
import com.rest.restaurantsystem.order.OrderPaymentRepository;
import com.rest.restaurantsystem.order.PaymentMethod;
import com.rest.restaurantsystem.realtime.RealtimeEventPublisher;
import com.rest.restaurantsystem.realtime.RealtimeEventType;
import com.rest.restaurantsystem.structure.BranchAssignmentService;
import com.rest.restaurantsystem.structure.BranchResponse;
import com.rest.restaurantsystem.structure.BranchService;
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
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CashRegisterServiceTest {

    @Mock
    private CashRegisterSessionRepository sessionRepository;

    @Mock
    private CashRegisterMovementRepository movementRepository;

    @Mock
    private OrderPaymentRepository paymentRepository;

    @Mock
    private BranchService branchService;

    @Mock
    private BranchAssignmentService assignmentService;

    @Mock
    private UserService userService;

    @Mock
    private RealtimeEventPublisher realtimeEventPublisher;

    @InjectMocks
    private CashRegisterService service;

    @Test
    void opensOneCashRegisterForTheAssignedBranch() {
        mockAuthorizedUser();
        mockSummaryNames();
        when(sessionRepository.findByBranchIdAndStatus(1L, CashRegisterStatus.OPEN))
                .thenReturn(Optional.empty());
        when(sessionRepository.saveAndFlush(any(CashRegisterSession.class)))
                .thenAnswer(invocation -> persisted(invocation.getArgument(0), 12L));

        CashRegisterSummaryResponse result = service.open(
                new CashRegisterOpenRequest(1L, new BigDecimal("500.00"), "Turno matutino"),
                "cajero"
        );

        assertThat(result.session().folio()).isEqualTo("CAJ-000012");
        assertThat(result.expectedCash()).isEqualByComparingTo("500.00");
        assertThat(result.canClose()).isTrue();
        verify(realtimeEventPublisher).publish(
                RealtimeEventType.CASH_REGISTER_CHANGED,
                1L,
                null,
                null,
                null
        );
    }

    @Test
    void registersACashOutAndKeepsItsAuditData() {
        CashRegisterSession session = persisted(session("500.00"), 12L);
        UUID operationId = UUID.randomUUID();
        mockAuthorizedUser();
        mockSummaryNames();
        when(sessionRepository.findLockedById(12L)).thenReturn(Optional.of(session));
        when(movementRepository.findBySessionIdAndOperationId(12L, operationId))
                .thenReturn(Optional.empty());
        when(movementRepository.sumActiveByType(12L, CashMovementType.CASH_OUT))
                .thenReturn(BigDecimal.ZERO, new BigDecimal("75.00"));
        when(movementRepository.saveAndFlush(any(CashRegisterMovement.class)))
                .thenAnswer(invocation -> {
                    CashRegisterMovement movement = invocation.getArgument(0);
                    ReflectionTestUtils.setField(movement, "id", 90L);
                    movement.onCreate();
                    return movement;
                });
        when(sessionRepository.saveAndFlush(session)).thenReturn(session);

        CashRegisterSummaryResponse result = service.addMovement(
                12L,
                new CashMovementCreateRequest(
                        operationId,
                        CashMovementType.CASH_OUT,
                        new BigDecimal("75.00"),
                        "Compra de hielo",
                        0
                ),
                "cajero"
        );

        assertThat(result.expectedCash()).isEqualByComparingTo("425.00");
        verify(movementRepository).saveAndFlush(any(CashRegisterMovement.class));
    }

    @Test
    void rejectsClosingWhileACollectedOrderRemainsOpen() {
        CashRegisterSession session = persisted(session("500.00"), 12L);
        mockAuthorizedUser();
        when(sessionRepository.findLockedById(12L)).thenReturn(Optional.of(session));
        when(paymentRepository.existsUnclosedOrderWithActivePayment(12L)).thenReturn(true);

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> service.close(
                        12L,
                        new CashRegisterCloseRequest(0, new BigDecimal("500.00"), null),
                        "cajero"
                )
        );

        assertThat(exception.getMessage()).contains("pedidos con cobros activos");
    }

    @Test
    void rejectsVoidingAnEntryThatWouldLeaveNegativeExpectedCash() {
        CashRegisterSession session = persisted(session("500.00"), 12L);
        CashRegisterMovement movement = new CashRegisterMovement(
                UUID.randomUUID(),
                12L,
                CashMovementType.CASH_IN,
                new BigDecimal("600.00"),
                "Cambio adicional",
                10L
        );
        ReflectionTestUtils.setField(movement, "id", 90L);
        movement.onCreate();
        mockAuthorizedUser();
        when(sessionRepository.findLockedById(12L)).thenReturn(Optional.of(session));
        when(movementRepository.findById(90L)).thenReturn(Optional.of(movement));
        when(movementRepository.sumActiveByType(12L, CashMovementType.CASH_IN))
                .thenReturn(new BigDecimal("600.00"));
        when(movementRepository.sumActiveByType(12L, CashMovementType.CASH_OUT))
                .thenReturn(new BigDecimal("700.00"));

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> service.voidMovement(
                        12L,
                        90L,
                        new CashMovementVoidRequest(0, "Captura incorrecta"),
                        "cajero"
                )
        );

        assertThat(exception.getMessage()).contains("Anula primero las salidas");
    }

    @Test
    void closesWithExpectedCountedAndDifferenceAmounts() {
        CashRegisterSession session = persisted(session("500.00"), 12L);
        mockAuthorizedUser();
        mockSummaryNames();
        when(sessionRepository.findLockedById(12L)).thenReturn(Optional.of(session));
        when(paymentRepository.sumActiveByCashRegisterAndMethod(12L, PaymentMethod.CASH))
                .thenReturn(new BigDecimal("300.00"));
        when(movementRepository.sumActiveByType(12L, CashMovementType.CASH_IN))
                .thenReturn(new BigDecimal("50.00"));
        when(movementRepository.sumActiveByType(12L, CashMovementType.CASH_OUT))
                .thenReturn(new BigDecimal("20.00"));
        when(sessionRepository.saveAndFlush(session)).thenReturn(session);

        CashRegisterSummaryResponse result = service.close(
                12L,
                new CashRegisterCloseRequest(0, new BigDecimal("825.00"), "Faltante revisado"),
                "cajero"
        );

        assertThat(result.session().status()).isEqualTo(CashRegisterStatus.CLOSED);
        assertThat(result.session().expectedCashAtClose()).isEqualByComparingTo("830.00");
        assertThat(result.session().countedCashAtClose()).isEqualByComparingTo("825.00");
        assertThat(result.session().differenceAtClose()).isEqualByComparingTo("-5.00");
    }

    private void mockAuthorizedUser() {
        when(userService.currentUser("cajero")).thenReturn(user());
        when(assignmentService.isAssigned(1L, 10L)).thenReturn(true);
    }

    private void mockSummaryNames() {
        when(branchService.findById(1L)).thenReturn(branch());
        when(userService.findById(10L)).thenReturn(user());
    }

    private CashRegisterSession session(String openingAmount) {
        return new CashRegisterSession(
                1L,
                new BigDecimal(openingAmount),
                null,
                10L
        );
    }

    private CashRegisterSession persisted(CashRegisterSession session, Long id) {
        ReflectionTestUtils.setField(session, "id", id);
        session.onCreate();
        return session;
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

    private BranchResponse branch() {
        return new BranchResponse(
                1L,
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
}
