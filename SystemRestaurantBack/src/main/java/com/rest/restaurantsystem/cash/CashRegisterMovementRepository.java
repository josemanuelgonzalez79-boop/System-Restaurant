package com.rest.restaurantsystem.cash;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface CashRegisterMovementRepository extends JpaRepository<CashRegisterMovement, Long> {

    List<CashRegisterMovement> findBySessionIdOrderByRecordedAtAscIdAsc(Long sessionId);

    Optional<CashRegisterMovement> findBySessionIdAndOperationId(Long sessionId, UUID operationId);

    @Query("""
            SELECT COALESCE(SUM(movement.amount), 0)
            FROM CashRegisterMovement movement
            WHERE movement.sessionId = :sessionId
              AND movement.movementType = :movementType
              AND movement.status = com.rest.restaurantsystem.cash.CashMovementStatus.ACTIVE
            """)
    BigDecimal sumActiveByType(
            @Param("sessionId") Long sessionId,
            @Param("movementType") CashMovementType movementType
    );
}
