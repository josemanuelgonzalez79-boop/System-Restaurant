package com.rest.restaurantsystem.order;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

interface PreparationItemRepository extends JpaRepository<PreparationItem, Long> {

    @Query("SELECT item.ticketId FROM PreparationItem item WHERE item.id = :id")
    Optional<Long> findTicketIdById(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT item FROM PreparationItem item WHERE item.id = :id")
    Optional<PreparationItem> findLockedById(@Param("id") Long id);

    List<PreparationItem> findByTicketIdInOrderByTicketIdAscCreatedAtAscIdAsc(
            Collection<Long> ticketIds
    );

    @Query(value = """
            SELECT EXISTS (
                SELECT 1
                FROM preparation_items item
                JOIN preparation_tickets ticket ON ticket.id = item.ticket_id
                WHERE ticket.order_id = :orderId
                  AND item.status IN ('PENDING', 'IN_PREPARATION', 'READY')
            )
            """, nativeQuery = true)
    boolean existsActiveByOrderId(@Param("orderId") Long orderId);
}
