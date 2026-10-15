package com.rest.restaurantsystem.reservation;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

interface ReservationRepository extends JpaRepository<Reservation, Long> {
    List<Reservation> findByStartAtGreaterThanEqualAndStartAtLessThanOrderByStartAtAscIdAsc(
            Instant start, Instant end);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Reservation r where r.id = :id")
    Optional<Reservation> findLocked(@Param("id") Long id);

    @Query("""
            select count(r) > 0 from Reservation r
            where r.servicePointId = :pointId
              and r.status in :statuses and r.startAt < :end and r.endAt > :start
              and r.id <> :excludeId
            """)
    boolean overlaps(@Param("pointId") Long pointId,
                     @Param("statuses") List<ReservationStatus> statuses,
                     @Param("start") Instant start,
                     @Param("end") Instant end,
                     @Param("excludeId") Long excludeId);

    boolean existsByServicePointIdAndStatusAndEndAtAfter(
            Long servicePointId, ReservationStatus status, Instant time);

    @Query("""
            select distinct r.servicePointId from Reservation r
            where r.branchId = :branchId and r.status = :status
              and r.startAt < :end and r.endAt > :start
            """)
    List<Long> heldPoints(@Param("branchId") Long branchId,
                          @Param("status") ReservationStatus status,
                          @Param("start") Instant start, @Param("end") Instant end);

    @Query(value = """
            select exists (select 1 from reservations r
                join service_points p on p.id = r.service_point_id
                where p.area_id = :areaId and r.status = 'BOOKED' and r.end_at > :now)
            """, nativeQuery = true)
    boolean existsUpcomingByArea(@Param("areaId") Long areaId, @Param("now") Instant now);
}
