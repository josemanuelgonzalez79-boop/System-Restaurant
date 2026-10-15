package com.rest.restaurantsystem.reservation;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

interface ReservationEventRepository extends JpaRepository<ReservationEvent, Long> {
    List<ReservationEvent> findByReservationIdOrderByOccurredAtAscIdAsc(Long reservationId);
}
