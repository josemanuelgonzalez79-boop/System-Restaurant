package com.rest.restaurantsystem.promotion;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

interface PromotionRepository extends JpaRepository<Promotion, Long> {
    List<Promotion> findAllByOrderByNameAscIdAsc();
    boolean existsByCodeAndIdNot(String code, Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Promotion p where p.id = :id")
    Optional<Promotion> findLocked(@Param("id") Long id);
}
