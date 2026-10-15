package com.rest.restaurantsystem.reservation;

import com.rest.restaurantsystem.customer.CustomerResponse;
import com.rest.restaurantsystem.customer.CustomerService;
import com.rest.restaurantsystem.exception.BadRequestException;
import com.rest.restaurantsystem.exception.ConflictException;
import com.rest.restaurantsystem.exception.ResourceNotFoundException;
import com.rest.restaurantsystem.order.OrderOccupancyService;
import com.rest.restaurantsystem.realtime.RealtimeEventPublisher;
import com.rest.restaurantsystem.realtime.RealtimeEventType;
import com.rest.restaurantsystem.structure.BranchResponse;
import com.rest.restaurantsystem.structure.BranchService;
import com.rest.restaurantsystem.structure.OperationalAreaService;
import com.rest.restaurantsystem.structure.ServicePointResponse;
import com.rest.restaurantsystem.structure.ServicePointService;
import com.rest.restaurantsystem.structure.ServicePointType;
import com.rest.restaurantsystem.user.UserService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;

@Service
public class ReservationService {
    private static final List<ReservationStatus> HELD = List.of(
            ReservationStatus.BOOKED, ReservationStatus.SEATED);
    private final ReservationRepository reservations;
    private final ReservationEventRepository events;
    private final BranchService branches;
    private final ServicePointService points;
    private final OperationalAreaService areas;
    private final OrderOccupancyService occupancy;
    private final UserService users;
    private final RealtimeEventPublisher realtime;
    private final CustomerService customers;

    public ReservationService(ReservationRepository reservations, ReservationEventRepository events,
                              BranchService branches, ServicePointService points,
                              OperationalAreaService areas, OrderOccupancyService occupancy,
                              UserService users, RealtimeEventPublisher realtime,
                              CustomerService customers) {
        this.reservations = reservations;
        this.events = events;
        this.branches = branches;
        this.points = points;
        this.areas = areas;
        this.occupancy = occupancy;
        this.users = users;
        this.realtime = realtime;
        this.customers = customers;
    }

    @Transactional(readOnly = true)
    public List<ReservationResponse> findDay(LocalDate date) {
        BranchResponse branch = localBranch();
        ZoneId zone = ZoneId.of(branch.timezone());
        return reservations.findByStartAtGreaterThanEqualAndStartAtLessThanOrderByStartAtAscIdAsc(
                date.atStartOfDay(zone).toInstant(), date.plusDays(1).atStartOfDay(zone).toInstant())
                .stream().map(item -> response(item, branch)).toList();
    }

    @Transactional(readOnly = true)
    public ReservationResponse find(Long id) {
        return response(get(id), localBranch());
    }

    @Transactional(readOnly = true)
    public List<ReservationEventResponse> history(Long id) {
        get(id);
        return events.findByReservationIdOrderByOccurredAtAscIdAsc(id)
                .stream().map(ReservationEventResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<Long> heldPoints(Long branchId) {
        Instant now = Instant.now();
        return reservations.heldPoints(branchId, ReservationStatus.BOOKED,
                now, now.plusSeconds(90 * 60L));
    }

    @Transactional
    public ReservationResponse create(ReservationSaveRequest request, String username) {
        request = withCustomerSnapshot(request);
        BranchResponse branch = localBranch();
        Period period = validatePeriod(request, branch);
        validatePoint(branch.id(), request.servicePointId());
        ensureNoOverlap(request.servicePointId(), period, null);
        if (period.start().isBefore(Instant.now().plusSeconds(90 * 60L))
                && occupancy.hasActiveOrderForServicePoint(request.servicePointId())) {
            throw new ConflictException("Esa mesa tiene un pedido activo. Elige otra mesa u horario.");
        }
        Long userId = users.currentUser(username).id();
        try {
            Reservation saved = reservations.saveAndFlush(new Reservation(
                    branch.id(), request, period.start(), period.end(), userId, username));
            record(saved.getId(), "CREATED", "Reservación creada para " + request.startLocal(), userId, username);
            publish(saved);
            return response(saved, branch);
        } catch (DataIntegrityViolationException exception) {
            throw new ConflictException("Esa mesa ya está reservada en el horario seleccionado.");
        }
    }

    @Transactional
    public ReservationResponse update(Long id, ReservationSaveRequest request, String username) {
        request = withCustomerSnapshot(request);
        BranchResponse branch = localBranch();
        Period period = validatePeriod(request, branch);
        validatePoint(branch.id(), request.servicePointId());
        Reservation current = locked(id);
        requireBookable(current, request.expectedVersion());
        ensureNoOverlap(request.servicePointId(), period, id);
        if (period.start().isBefore(Instant.now().plusSeconds(90 * 60L))
                && occupancy.hasActiveOrderForServicePoint(request.servicePointId())) {
            throw new ConflictException("Esa mesa tiene un pedido activo. Elige otra mesa u horario.");
        }
        String detail = "Antes " + current.getStartAt() + " / mesa #" + current.getServicePointId()
                + "; ahora " + period.start() + " / mesa #" + request.servicePointId();
        current.reschedule(request, period.start(), period.end(), username);
        try {
            Reservation saved = reservations.saveAndFlush(current);
            record(id, "RESCHEDULED", detail, users.currentUser(username).id(), username);
            publish(saved);
            return response(saved, branch);
        } catch (DataIntegrityViolationException exception) {
            throw new ConflictException("Esa mesa ya está reservada en el horario seleccionado.");
        }
    }

    @Transactional
    public ReservationResponse cancel(Long id, ReservationActionRequest request, String username) {
        Reservation reservation = locked(id);
        requireBookable(reservation, request.expectedVersion());
        if (request.reason() == null || request.reason().trim().length() < 5) {
            throw new BadRequestException("Escribe un motivo de cancelación de al menos 5 caracteres.");
        }
        reservation.cancel(request.reason(), username);
        Reservation saved = reservations.saveAndFlush(reservation);
        record(id, "CANCELLED", request.reason().trim(), users.currentUser(username).id(), username);
        publish(saved);
        return response(saved, localBranch());
    }

    @Transactional
    public ReservationResponse noShow(Long id, ReservationActionRequest request, String username) {
        Reservation reservation = locked(id);
        requireBookable(reservation, request.expectedVersion());
        if (Instant.now().isBefore(reservation.getStartAt().plusSeconds(15 * 60L))) {
            throw new BadRequestException("Espera 15 minutos después de la hora reservada para marcar ausencia.");
        }
        reservation.markNoShow(username);
        Reservation saved = reservations.saveAndFlush(reservation);
        record(id, "NO_SHOW", "No se presentó después de 15 minutos", users.currentUser(username).id(), username);
        publish(saved);
        return response(saved, localBranch());
    }

    /** Called by OrderService inside its order-creation transaction, after the point row is locked. */
    @Transactional
    public Long beforeOrder(Long reservationId, Long branchId, Long pointId) {
        if (reservationId == null) {
            Instant now = Instant.now();
            if (reservations.overlaps(pointId, List.of(ReservationStatus.BOOKED),
                    now, now.plusSeconds(90 * 60L), -1L)) {
                throw new ConflictException("Esta mesa está reservada próximamente. Abre la cuenta desde Reservaciones.");
            }
            return null;
        }
        Reservation reservation = locked(reservationId);
        if (reservation.getStatus() != ReservationStatus.BOOKED
                || !reservation.getBranchId().equals(branchId)
                || !reservation.getServicePointId().equals(pointId)) {
            throw new ConflictException("La reservación ya cambió o no pertenece a esta mesa.");
        }
        Instant now = Instant.now();
        if (now.isBefore(reservation.getStartAt().minusSeconds(30 * 60L))
                || !now.isBefore(reservation.getEndAt())) {
            throw new BadRequestException("Puedes sentar al cliente desde 30 minutos antes y hasta el fin del horario reservado.");
        }
        return reservation.getCustomerId();
    }

    @Transactional
    public void afterOrder(Long reservationId, Long orderId, String username) {
        if (reservationId == null) return;
        Reservation reservation = locked(reservationId);
        if (reservation.getStatus() != ReservationStatus.BOOKED) {
            throw new ConflictException("La reservación ya fue utilizada o cancelada.");
        }
        reservation.seat(orderId, username);
        reservations.saveAndFlush(reservation);
        record(reservationId, "SEATED", "Pedido PED-%06d".formatted(orderId),
                users.currentUser(username).id(), username);
        publish(reservation);
    }

    private void requireBookable(Reservation reservation, Long version) {
        if (version == null || reservation.getVersion() != version) {
            throw new ConflictException("La reservación cambió en otra tablet. Actualiza la agenda.");
        }
        if (reservation.getStatus() != ReservationStatus.BOOKED) {
            throw new BadRequestException("Solo es posible modificar una reservación pendiente.");
        }
    }

    private Period validatePeriod(ReservationSaveRequest request, BranchResponse branch) {
        LocalDateTime local = request.startLocal();
        if (local.getSecond() != 0 || local.getNano() != 0 || local.getMinute() % 15 != 0
                || request.durationMinutes() % 15 != 0) {
            throw new BadRequestException("El horario y la duración deben estar en intervalos de 15 minutos.");
        }
        ZoneId zone = ZoneId.of(branch.timezone());
        List<ZoneOffset> offsets = zone.getRules().getValidOffsets(local);
        if (offsets.size() != 1) {
            throw new BadRequestException("Esta hora local no existe o se repite por el cambio de horario.");
        }
        Instant start = local.atOffset(offsets.getFirst()).toInstant();
        if (start.isBefore(Instant.now()) || start.isAfter(Instant.now().plusSeconds(90L * 24 * 3600))) {
            throw new BadRequestException("Reserva una fecha futura dentro de los próximos 90 días.");
        }
        return new Period(start, start.plusSeconds(request.durationMinutes() * 60L));
    }

    private void validatePoint(Long branchId, Long pointId) {
        // Lock area before point to serialize structure changes without reversing lock order.
        ServicePointResponse initial = points.findById(pointId);
        areas.lockArea(initial.areaId());
        points.lockPoint(pointId);
        ServicePointResponse point = points.findById(pointId);
        if (!point.areaId().equals(initial.areaId())) {
            throw new ConflictException("La mesa cambió de área. Actualiza la agenda e intenta nuevamente.");
        }
        if (!point.branchId().equals(branchId) || !point.active()
                || !areas.findById(point.areaId()).active()
                || point.pointType() != ServicePointType.TABLE && point.pointType() != ServicePointType.ROOM) {
            throw new BadRequestException("Selecciona una mesa o salón activo del restaurante.");
        }
    }

    private void ensureNoOverlap(Long pointId, Period period, Long excludedId) {
        if (reservations.overlaps(pointId, HELD, period.start(), period.end(),
                excludedId == null ? -1L : excludedId)) {
            throw new ConflictException("La mesa ya tiene una reservación en ese horario.");
        }
    }

    private ReservationSaveRequest withCustomerSnapshot(ReservationSaveRequest request) {
        if (request.customerId() == null) return request;
        CustomerResponse customer = customers.requireActive(request.customerId());
        return new ReservationSaveRequest(request.servicePointId(), request.startLocal(),
                request.durationMinutes(), customer.name(), customer.phone(), request.guestCount(),
                request.notes(), customer.id(), request.expectedVersion());
    }

    private BranchResponse localBranch() {
        return branches.findAll().stream().findFirst()
                .orElseThrow(() -> new BadRequestException("Configura primero el restaurante local."));
    }

    private Reservation get(Long id) {
        return reservations.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró la reservación."));
    }

    private Reservation locked(Long id) {
        return reservations.findLocked(id)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró la reservación."));
    }

    private ReservationResponse response(Reservation item, BranchResponse branch) {
        ServicePointResponse point = points.findById(item.getServicePointId());
        return new ReservationResponse(item.getId(), item.getBranchId(), point.id(), item.getCustomerId(),
                point.name(), point.areaName(), item.getGuestName(), item.getGuestPhone(),
                item.getGuestCount(), item.getNotes(), item.getStartAt(), item.getEndAt(),
                branch.timezone(), item.getStatus(), item.getOrderId(),
                item.getCreatedByUsername(), item.getUpdatedByUsername(), item.getCancelReason(),
                item.getVersion(), item.getCreatedAt(), item.getUpdatedAt());
    }

    private void record(Long id, String action, String detail, Long userId, String username) {
        events.save(new ReservationEvent(id, action, detail, userId, username));
    }

    private void publish(Reservation item) {
        realtime.publish(RealtimeEventType.RESERVATION_CHANGED, item.getBranchId(), null, null, null);
    }

    private record Period(Instant start, Instant end) { }
}
