package com.rest.restaurantsystem.customer;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

interface CustomerEventRepository extends JpaRepository<CustomerEvent, Long> {
    List<CustomerEvent> findTop50ByCustomerIdOrderByOccurredAtDescIdDesc(Long customerId);
}
