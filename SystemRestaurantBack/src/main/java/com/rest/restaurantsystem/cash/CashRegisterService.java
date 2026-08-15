package com.rest.restaurantsystem.cash;

import com.rest.restaurantsystem.exception.BadRequestException;
import com.rest.restaurantsystem.exception.ConflictException;
import com.rest.restaurantsystem.exception.ResourceNotFoundException;
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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class CashRegisterService {

    private static final Set<UserRole> CASH_ROLES =
            EnumSet.of(UserRole.OWNER, UserRole.ADMIN, UserRole.MANAGER, UserRole.CASHIER);

    private final CashRegisterSessionRepository sessionRepository;
    private final CashRegisterMovementRepository movementRepository;
    private final OrderPaymentRepository paymentRepository;
    private final BranchService branchService;
    private final BranchAssignmentService assignmentService;
    private final UserService userService;
    private final RealtimeEventPublisher realtimeEventPublisher;

    public CashRegisterService(
            CashRegisterSessionRepository sessionRepository,
            CashRegisterMovementRepository movementRepository,
            OrderPaymentRepository paymentRepository,
            BranchService branchService,
            BranchAssignmentService assignmentService,
            UserService userService,
            RealtimeEventPublisher realtimeEventPublisher
    ) {
        this.sessionRepository = sessionRepository;
        this.movementRepository = movementRepository;
        this.paymentRepository = paymentRepository;
        this.branchService = branchService;
        this.assignmentService = assignmentService;
        this.userService = userService;
        this.realtimeEventPublisher = realtimeEventPublisher;
    }

    @Transactional(readOnly = true)
    public Optional<CashRegisterSummaryResponse> findCurrent(
            Long branchId,
            String currentUsername
    ) {
        authorizedUser(branchId, currentUsername);
        return sessionRepository.findByBranchIdAndStatus(branchId, CashRegisterStatus.OPEN)
                .map(this::buildSummary);
    }

    @Transactional(readOnly = true)
    public List<CashRegisterSummaryResponse> findHistory(
            Long branchId,
            String currentUsername
    ) {
        authorizedUser(branchId, currentUsername);
        return sessionRepository.findTop30ByBranchIdOrderByOpenedAtDesc(branchId).stream()
                .map(this::buildSummary)
                .toList();
    }

    @Transactional(readOnly = true)
    public CashRegisterSummaryResponse findById(Long id, String currentUsername) {
        CashRegisterSession session = getSession(id);
        authorizedUser(session.getBranchId(), currentUsername);
        return buildSummary(session);
    }

    @Transactional
    public CashRegisterSummaryResponse open(
            CashRegisterOpenRequest request,
            String currentUsername
    ) {
        UserResponse user = authorizedUser(request.branchId(), currentUsername);
        BranchResponse branch = branchService.findById(request.branchId());
        if (!branch.active()) {
            throw new BadRequestException("La sucursal debe estar activa para abrir caja.");
        }
        if (sessionRepository.findByBranchIdAndStatus(
                request.branchId(),
                CashRegisterStatus.OPEN
        ).isPresent()) {
            throw new ConflictException("La sucursal ya tiene una caja abierta.");
        }

        try {
            CashRegisterSession saved = sessionRepository.saveAndFlush(
                    new CashRegisterSession(
                            request.branchId(),
                            request.openingAmount(),
                            request.notes(),
                            user.id()
                    )
            );
            CashRegisterSummaryResponse response = buildSummary(saved);
            publishChanged(saved);
            return response;
        } catch (DataIntegrityViolationException exception) {
            throw new ConflictException("La sucursal ya tiene una caja abierta.");
        }
    }

    @Transactional
    public CashRegisterSummaryResponse addMovement(
            Long sessionId,
            CashMovementCreateRequest request,
            String currentUsername
    ) {
        CashRegisterSession session = lockedSession(sessionId);
        UserResponse user = authorizedUser(session.getBranchId(), currentUsername);
        ensureOpen(session);

        if (movementRepository.findBySessionIdAndOperationId(
                sessionId,
                request.operationId()
        ).isPresent()) {
            return buildSummary(session);
        }
        ensureVersion(session, request.sessionVersion());

        if (request.movementType() == CashMovementType.CASH_OUT
                && request.amount().compareTo(calculateExpectedCash(session)) > 0) {
            throw new BadRequestException(
                    "La salida no puede superar el efectivo esperado en caja."
            );
        }

        movementRepository.saveAndFlush(
                new CashRegisterMovement(
                        request.operationId(),
                        sessionId,
                        request.movementType(),
                        request.amount(),
                        request.concept(),
                        user.id()
                )
        );
        session.touch();
        sessionRepository.saveAndFlush(session);

        CashRegisterSummaryResponse response = buildSummary(session);
        publishChanged(session);
        return response;
    }

    @Transactional
    public CashRegisterSummaryResponse voidMovement(
            Long sessionId,
            Long movementId,
            CashMovementVoidRequest request,
            String currentUsername
    ) {
        CashRegisterSession session = lockedSession(sessionId);
        UserResponse user = authorizedUser(session.getBranchId(), currentUsername);
        ensureOpen(session);
        ensureVersion(session, request.sessionVersion());

        CashRegisterMovement movement = movementRepository.findById(movementId)
                .filter(candidate -> candidate.getSessionId().equals(sessionId))
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontró el movimiento de caja solicitado."
                ));
        if (movement.getStatus() == CashMovementStatus.VOIDED) {
            throw new BadRequestException("El movimiento de caja ya fue anulado.");
        }
        if (movement.getMovementType() == CashMovementType.CASH_IN
                && movement.getAmount().compareTo(calculateExpectedCash(session)) > 0) {
            throw new BadRequestException(
                    "Anula primero las salidas relacionadas; esta entrada ya respalda efectivo utilizado."
            );
        }
        movement.voidMovement(user.id(), request.reason());
        movementRepository.saveAndFlush(movement);
        session.touch();
        sessionRepository.saveAndFlush(session);

        CashRegisterSummaryResponse response = buildSummary(session);
        publishChanged(session);
        return response;
    }

    @Transactional
    public CashRegisterSummaryResponse close(
            Long sessionId,
            CashRegisterCloseRequest request,
            String currentUsername
    ) {
        CashRegisterSession session = lockedSession(sessionId);
        UserResponse user = authorizedUser(session.getBranchId(), currentUsername);
        ensureOpen(session);
        ensureVersion(session, request.sessionVersion());

        String blockingReason = closeBlockingReason(session);
        if (blockingReason != null) {
            throw new BadRequestException(blockingReason);
        }

        BigDecimal expectedCash = calculateExpectedCash(session);
        session.close(expectedCash, request.countedCash(), user.id(), request.notes());
        sessionRepository.saveAndFlush(session);

        CashRegisterSummaryResponse response = buildSummary(session);
        publishChanged(session);
        return response;
    }

    @Transactional
    public CashRegisterReference lockOpenForPayment(Long branchId) {
        CashRegisterSession session = sessionRepository.findLockedOpenByBranchId(branchId)
                .orElseThrow(() -> new BadRequestException(
                        "Abre la caja de la sucursal antes de registrar cobros."
                ));
        return toReference(session);
    }

    @Transactional
    public void ensurePaymentSessionOpen(Long sessionId) {
        if (sessionId == null) {
            return;
        }
        CashRegisterSession session = lockedSession(sessionId);
        if (session.getStatus() != CashRegisterStatus.OPEN) {
            throw new BadRequestException(
                    "El cobro pertenece a un turno de caja cerrado y ya no puede anularse."
            );
        }
    }

    @Transactional(readOnly = true)
    public Optional<CashRegisterReference> findOpenReference(Long branchId) {
        return sessionRepository.findByBranchIdAndStatus(branchId, CashRegisterStatus.OPEN)
                .map(this::toReference);
    }

    private CashRegisterSession lockedSession(Long id) {
        return sessionRepository.findLockedById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontró el turno de caja solicitado."
                ));
    }

    private CashRegisterSession getSession(Long id) {
        return sessionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontró el turno de caja solicitado."
                ));
    }

    private UserResponse authorizedUser(Long branchId, String username) {
        UserResponse user = userService.currentUser(username);
        if (!user.active()) {
            throw new BadRequestException("Tu usuario está inactivo.");
        }
        if (!CASH_ROLES.contains(user.role())) {
            throw new BadRequestException(
                    "Solo propietario, administrador, gerente o cajero puede operar caja."
            );
        }
        if (!assignmentService.isAssigned(branchId, user.id())) {
            throw new BadRequestException(
                    "Tu usuario no está asignado a la sucursal seleccionada."
            );
        }
        return user;
    }

    private void ensureOpen(CashRegisterSession session) {
        if (session.getStatus() != CashRegisterStatus.OPEN) {
            throw new BadRequestException("El turno de caja ya está cerrado.");
        }
    }

    private void ensureVersion(CashRegisterSession session, long requestedVersion) {
        if (session.getVersion() != requestedVersion) {
            throw new ConflictException(
                    "La caja cambió en otro dispositivo. Actualiza antes de continuar."
            );
        }
    }

    private CashRegisterSummaryResponse buildSummary(CashRegisterSession session) {
        BigDecimal cashPayments = paymentTotal(session.getId(), PaymentMethod.CASH);
        BigDecimal cardPayments = paymentTotal(session.getId(), PaymentMethod.CARD);
        BigDecimal transferPayments = paymentTotal(session.getId(), PaymentMethod.TRANSFER);
        BigDecimal otherPayments = paymentTotal(session.getId(), PaymentMethod.OTHER);
        BigDecimal cashIn = movementTotal(session.getId(), CashMovementType.CASH_IN);
        BigDecimal cashOut = movementTotal(session.getId(), CashMovementType.CASH_OUT);
        BigDecimal expectedCash = session.getStatus() == CashRegisterStatus.CLOSED
                ? session.getExpectedCashAtClose()
                : session.getOpeningAmount().add(cashPayments).add(cashIn).subtract(cashOut);
        String blockingReason = closeBlockingReason(session);

        return new CashRegisterSummaryResponse(
                toSessionResponse(session),
                movementRepository.findBySessionIdOrderByRecordedAtAscIdAsc(session.getId())
                        .stream()
                        .map(this::toMovementResponse)
                        .toList(),
                cashPayments,
                cardPayments,
                transferPayments,
                otherPayments,
                cashIn,
                cashOut,
                expectedCash,
                blockingReason == null,
                blockingReason
        );
    }

    private BigDecimal calculateExpectedCash(CashRegisterSession session) {
        return session.getOpeningAmount()
                .add(paymentTotal(session.getId(), PaymentMethod.CASH))
                .add(movementTotal(session.getId(), CashMovementType.CASH_IN))
                .subtract(movementTotal(session.getId(), CashMovementType.CASH_OUT));
    }

    private BigDecimal paymentTotal(Long sessionId, PaymentMethod method) {
        BigDecimal total = paymentRepository.sumActiveByCashRegisterAndMethod(sessionId, method);
        return total == null ? BigDecimal.ZERO : total;
    }

    private BigDecimal movementTotal(Long sessionId, CashMovementType movementType) {
        BigDecimal total = movementRepository.sumActiveByType(sessionId, movementType);
        return total == null ? BigDecimal.ZERO : total;
    }

    private String closeBlockingReason(CashRegisterSession session) {
        if (session.getStatus() == CashRegisterStatus.CLOSED) {
            return "El turno de caja ya está cerrado.";
        }
        if (paymentRepository.existsUnclosedOrderWithActivePayment(session.getId())) {
            return "Hay pedidos con cobros activos que todavía no han sido cerrados.";
        }
        return null;
    }

    private CashRegisterSessionResponse toSessionResponse(CashRegisterSession session) {
        BranchResponse branch = branchService.findById(session.getBranchId());
        UserResponse openedBy = userService.findById(session.getOpenedByUserId());
        UserResponse closedBy = session.getClosedByUserId() == null
                ? null
                : userService.findById(session.getClosedByUserId());
        return new CashRegisterSessionResponse(
                session.getId(),
                folio(session.getId()),
                session.getBranchId(),
                branch.name(),
                session.getOpeningAmount(),
                session.getOpeningNotes(),
                session.getStatus(),
                session.getOpenedByUserId(),
                openedBy.fullName(),
                session.getOpenedAt(),
                session.getClosedByUserId(),
                closedBy == null ? null : closedBy.fullName(),
                session.getClosedAt(),
                session.getExpectedCashAtClose(),
                session.getCountedCashAtClose(),
                session.getDifferenceAtClose(),
                session.getClosingNotes(),
                session.getVersion(),
                session.getUpdatedAt()
        );
    }

    private CashMovementResponse toMovementResponse(CashRegisterMovement movement) {
        UserResponse recordedBy = userService.findById(movement.getRecordedByUserId());
        UserResponse voidedBy = movement.getVoidedByUserId() == null
                ? null
                : userService.findById(movement.getVoidedByUserId());
        return new CashMovementResponse(
                movement.getId(),
                "MOV-%06d".formatted(movement.getId()),
                movement.getOperationId(),
                movement.getMovementType(),
                movement.getAmount(),
                movement.getConcept(),
                movement.getStatus(),
                movement.getRecordedByUserId(),
                recordedBy.fullName(),
                movement.getRecordedAt(),
                movement.getVoidedByUserId(),
                voidedBy == null ? null : voidedBy.fullName(),
                movement.getVoidedAt(),
                movement.getVoidReason()
        );
    }

    private CashRegisterReference toReference(CashRegisterSession session) {
        return new CashRegisterReference(session.getId(), folio(session.getId()));
    }

    private String folio(Long id) {
        return "CAJ-%06d".formatted(id);
    }

    private void publishChanged(CashRegisterSession session) {
        realtimeEventPublisher.publish(
                RealtimeEventType.CASH_REGISTER_CHANGED,
                session.getBranchId(),
                null,
                null,
                null
        );
    }
}
