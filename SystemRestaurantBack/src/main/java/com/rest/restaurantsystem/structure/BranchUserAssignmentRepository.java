package com.rest.restaurantsystem.structure;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

interface BranchUserAssignmentRepository extends JpaRepository<BranchUserAssignment, Long> {

    List<BranchUserAssignment> findAllByBranchId(Long branchId);

    List<BranchUserAssignment> findAllByUserId(Long userId);

    boolean existsByBranchIdAndUserId(Long branchId, Long userId);

    void deleteAllByBranchId(Long branchId);
}
