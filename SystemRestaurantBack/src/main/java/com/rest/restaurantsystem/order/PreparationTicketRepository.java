package com.rest.restaurantsystem.order;

import com.rest.restaurantsystem.catalog.product.ProductDestination;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

interface PreparationTicketRepository extends JpaRepository<PreparationTicket, Long> {

    List<PreparationTicket> findByBranchIdOrderBySentAtAscIdAsc(Long branchId);

    List<PreparationTicket> findByBranchIdAndDestinationOrderBySentAtAscIdAsc(
            Long branchId,
            ProductDestination destination
    );

    long countByOrderIdAndDestination(Long orderId, ProductDestination destination);
}
