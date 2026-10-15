package com.rest.restaurantsystem.reservation;

import com.rest.restaurantsystem.customer.CustomerResponse;
import com.rest.restaurantsystem.customer.CustomerService;
import com.rest.restaurantsystem.exception.BadRequestException;
import com.rest.restaurantsystem.exception.ConflictException;
import com.rest.restaurantsystem.order.OrderOccupancyService;
import com.rest.restaurantsystem.realtime.RealtimeEventPublisher;
import com.rest.restaurantsystem.structure.BranchResponse;
import com.rest.restaurantsystem.structure.BranchService;
import com.rest.restaurantsystem.structure.OperationalAreaResponse;
import com.rest.restaurantsystem.structure.OperationalAreaService;
import com.rest.restaurantsystem.structure.ServicePointResponse;
import com.rest.restaurantsystem.structure.ServicePointService;
import com.rest.restaurantsystem.structure.ServicePointType;
import com.rest.restaurantsystem.structure.AreaType;
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
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {
    @Mock ReservationRepository reservations;
    @Mock ReservationEventRepository events;
    @Mock BranchService branches;
    @Mock ServicePointService points;
    @Mock OperationalAreaService areas;
    @Mock OrderOccupancyService occupancy;
    @Mock UserService users;
    @Mock RealtimeEventPublisher realtime;
    @Mock CustomerService customers;
    @InjectMocks ReservationService service;

    @Test
    void refusesOverlappingTableWithoutSaving() {
        when(branches.findAll()).thenReturn(List.of(branch()));
        when(points.findById(20L)).thenReturn(point());
        when(areas.findById(5L)).thenReturn(area());
        when(reservations.overlaps(eq(20L), any(), any(), any(), eq(-1L))).thenReturn(true);

        assertThrows(ConflictException.class, () -> service.create(request(), "caja"));
        verify(reservations, never()).saveAndFlush(any());
    }

    @Test
    void walkInCannotTakeTableReservedWithinNinetyMinutes() {
        when(reservations.overlaps(eq(20L), any(), any(), any(), eq(-1L))).thenReturn(true);
        assertThrows(ConflictException.class, () -> service.beforeOrder(null, 1L, 20L));
    }

    @Test
    void cannotMarkNoShowBeforeFifteenMinutes() {
        Reservation reservation = new Reservation(1L, request(),
                Instant.now().plusSeconds(3600), Instant.now().plusSeconds(7200), 4L, "caja");
        ReflectionTestUtils.setField(reservation, "id", 10L);
        when(reservations.findLocked(10L)).thenReturn(Optional.of(reservation));
        assertThrows(BadRequestException.class,
                () -> service.noShow(10L, new ReservationActionRequest(0L, null), "caja"));
        verify(reservations, never()).saveAndFlush(any());
    }

    @Test
    void seatingLinksSingleOrderAndKeepsAudit() {
        Reservation reservation = new Reservation(1L, request(),
                Instant.now().minusSeconds(300), Instant.now().plusSeconds(3600), 4L, "caja");
        ReflectionTestUtils.setField(reservation, "id", 10L);
        when(reservations.findLocked(10L)).thenReturn(Optional.of(reservation));
        when(reservations.saveAndFlush(reservation)).thenReturn(reservation);
        when(users.currentUser("caja")).thenReturn(user());

        service.beforeOrder(10L, 1L, 20L);
        service.afterOrder(10L, 42L, "caja");

        assertEquals(ReservationStatus.SEATED, reservation.getStatus());
        assertEquals(42L, reservation.getOrderId());
        verify(events).save(any(ReservationEvent.class));
        verify(realtime).publish(any(), eq(1L), any(), any(), any());
    }

    @Test
    void linkedProfileSnapshotsNameAndPhoneAtBooking() {
        ReservationSaveRequest linked = new ReservationSaveRequest(20L, request().startLocal(),
                90, "No usar este nombre", null, 2, null, 50L, null);
        when(customers.requireActive(50L)).thenReturn(new CustomerResponse(50L, "Ana",
                "667 123 4567", null, "Mesa tranquila", null, true, 0L, "caja", "caja",
                Instant.EPOCH, Instant.EPOCH));
        when(branches.findAll()).thenReturn(List.of(branch()));
        when(points.findById(20L)).thenReturn(point());
        when(areas.findById(5L)).thenReturn(area());
        when(users.currentUser("caja")).thenReturn(user());
        when(reservations.saveAndFlush(any(Reservation.class))).thenAnswer(invocation -> {
            Reservation saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 11L);
            return saved;
        });

        ReservationResponse response = service.create(linked, "caja");

        assertEquals(50L, response.customerId());
        assertEquals("Ana", response.guestName());
        assertEquals("667 123 4567", response.guestPhone());
    }

    private ReservationSaveRequest request() {
        LocalDateTime tomorrow = LocalDateTime.now(ZoneId.of("America/Mazatlan"))
                .plusDays(1).withHour(19).withMinute(0).withSecond(0).withNano(0);
        return new ReservationSaveRequest(20L, tomorrow, 90, "Ana", "6671234567", 2, null, null);
    }

    private BranchResponse branch() {
        Instant now = Instant.now();
        return new BranchResponse(1L, "PRINCIPAL", "Restaurante", null, null,
                "America/Mazatlan", 0, true, 0L, now, now);
    }

    private ServicePointResponse point() {
        Instant now = Instant.now();
        return new ServicePointResponse(20L, 5L, "Comedor", 1L, "Restaurante", "M01",
                "Mesa 1", null, ServicePointType.TABLE, 0, true, 0L, now, now);
    }

    private OperationalAreaResponse area() {
        Instant now = Instant.now();
        return new OperationalAreaResponse(5L, 1L, "Restaurante", "Comedor", null,
                AreaType.SERVICE, 0, true, 0L, now, now);
    }

    private UserResponse user() {
        Instant now = Instant.now();
        return new UserResponse(4L, "caja", "Caja", UserRole.CASHIER,
                true, false, 0L, now, now);
    }
}
