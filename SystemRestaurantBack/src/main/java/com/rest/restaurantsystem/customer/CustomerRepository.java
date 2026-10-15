package com.rest.restaurantsystem.customer;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

interface CustomerRepository extends JpaRepository<Customer, Long> {
    @Query("""
            select c from Customer c where (:archived = true or c.active = true)
              and (:term = '' or lower(c.name) like concat('%', :term, '%')
                   or (:digits <> '' and c.phoneKey like concat('%', :digits, '%')))
            """)
    Page<Customer> search(@Param("term") String term, @Param("digits") String digits,
                          @Param("archived") boolean archived, Pageable pageable);

    boolean existsByPhoneKeyAndIdNot(String phoneKey, Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Customer c where c.id = :id")
    Optional<Customer> findLocked(@Param("id") Long id);
}
