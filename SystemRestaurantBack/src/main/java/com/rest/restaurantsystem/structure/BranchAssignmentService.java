package com.rest.restaurantsystem.structure;

import com.rest.restaurantsystem.user.UserResponse;
import com.rest.restaurantsystem.user.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/** Compatibility boundary for the internal branch identifier used by orders and reports. */
@Service
public class BranchAssignmentService {

    private final BranchService branchService;
    private final UserService userService;

    public BranchAssignmentService(BranchService branchService, UserService userService) {
        this.branchService = branchService;
        this.userService = userService;
    }

    @Transactional(readOnly = true)
    public List<BranchAssignmentUserResponse> findAssignedActiveUsers(Long branchId) {
        requireLocalBranch(branchId);
        return userService.findAll().stream()
                .filter(UserResponse::active)
                .map(user -> BranchAssignmentUserResponse.from(user, true))
                .toList();
    }

    @Transactional(readOnly = true)
    public boolean isAssigned(Long branchId, Long userId) {
        return requireLocalBranch(branchId) && userService.findById(userId).active();
    }

    @Transactional(readOnly = true)
    public Set<Long> findAssignedBranchIds(Long userId) {
        if (!userService.findById(userId).active()) {
            return Set.of();
        }
        return branchService.findAll().stream()
                .filter(BranchResponse::active)
                .map(BranchResponse::id)
                .collect(Collectors.toSet());
    }

    private boolean requireLocalBranch(Long branchId) {
        return branchService.findById(branchId).active();
    }
}
