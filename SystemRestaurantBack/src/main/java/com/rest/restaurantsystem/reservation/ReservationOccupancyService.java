package com.rest.restaurantsystem.reservation;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class ReservationOccupancyService {
    private final ReservationRepository repository;

    public ReservationOccupancyService(ReservationRepository repository) { this.repository = repository; }

    @Transactional(readOnly = true)
    public boolean hasUpcomingForPoint(Long pointId) {
        return repository.existsByServicePointIdAndStatusAndEndAtAfter(
                pointId, ReservationStatus.BOOKED, Instant.now());
    }

    @Transactional(readOnly = true)
    public boolean hasUpcomingForArea(Long areaId) {
        return repository.existsUpcomingByArea(areaId, Instant.now());
    }
}
