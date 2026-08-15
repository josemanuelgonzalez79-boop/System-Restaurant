package com.rest.restaurantsystem.cash;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

interface CashRegisterSessionRepository extends JpaRepository<CashRegisterSession, Long> {

    Optional<CashRegisterSession> findByBranchIdAndStatus(
            Long branchId,
            CashRegisterStatus status
    );

    List<CashRegisterSession> findTop30ByBranchIdOrderByOpenedAtDesc(Long branchId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT session FROM CashRegisterSession session WHERE session.id = :id")
    Optional<CashRegisterSession> findLockedById(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT session
            FROM CashRegisterSession session
            WHERE session.branchId = :branchId
              AND session.status = com.rest.restaurantsystem.cash.CashRegisterStatus.OPEN
            """)
    Optional<CashRegisterSession> findLockedOpenByBranchId(@Param("branchId") Long branchId);
}
