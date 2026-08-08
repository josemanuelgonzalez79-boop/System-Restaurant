package com.rest.restaurantsystem.structure;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

interface BranchRepository extends JpaRepository<Branch, Long> {

    List<Branch> findAllByOrderBySortOrderAscNameAsc();

    boolean existsByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCaseAndIdNot(String code, Long id);

    long countByActiveTrue();
}
