package com.rest.restaurantsystem.structure;

import com.rest.restaurantsystem.exception.BadRequestException;
import com.rest.restaurantsystem.user.UserResponse;
import com.rest.restaurantsystem.user.UserRole;
import com.rest.restaurantsystem.user.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class BranchAssignmentService {

    private final BranchUserAssignmentRepository repository;
    private final BranchService branchService;
    private final UserService userService;

    public BranchAssignmentService(
            BranchUserAssignmentRepository repository,
            BranchService branchService,
            UserService userService
    ) {
        this.repository = repository;
        this.branchService = branchService;
        this.userService = userService;
    }

    @Transactional(readOnly = true)
    public List<BranchAssignmentUserResponse> findAll(Long branchId) {
        branchService.getEntity(branchId);
        Set<Long> assignedIds = repository.findAllByBranchId(branchId)
                .stream()
                .map(BranchUserAssignment::getUserId)
                .collect(Collectors.toSet());
        return userService.findAll()
                .stream()
                .map(user -> BranchAssignmentUserResponse.from(user, assignedIds.contains(user.id())))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<BranchAssignmentUserResponse> findAssignedActiveUsers(Long branchId) {
        return findAll(branchId).stream()
                .filter(BranchAssignmentUserResponse::active)
                .filter(BranchAssignmentUserResponse::assigned)
                .toList();
    }

    @Transactional(readOnly = true)
    public boolean isAssigned(Long branchId, Long userId) {
        return repository.existsByBranchIdAndUserId(branchId, userId);
    }

    @Transactional(readOnly = true)
    public Set<Long> findAssignedBranchIds(Long userId) {
        return repository.findAllByUserId(userId).stream()
                .map(BranchUserAssignment::getBranchId)
                .collect(Collectors.toSet());
    }

    @Transactional
    public List<BranchAssignmentUserResponse> replace(Long branchId, BranchAssignmentRequest request) {
        Branch branch = branchService.getEntity(branchId);
        if (!branch.isActive()) {
            throw new BadRequestException("La sucursal debe estar activa para asignar usuarios.");
        }

        Set<Long> requestedIds = new LinkedHashSet<>(request.userIds());
        Map<Long, UserResponse> activeUsers = userService.findAll()
                .stream()
                .filter(UserResponse::active)
                .collect(Collectors.toMap(UserResponse::id, Function.identity()));

        if (!activeUsers.keySet().containsAll(requestedIds)) {
            throw new BadRequestException("La asignación contiene usuarios inexistentes o inactivos.");
        }
        boolean hasOwner = requestedIds.stream()
                .map(activeUsers::get)
                .anyMatch(user -> user.role() == UserRole.OWNER);
        if (!hasOwner) {
            throw new BadRequestException("Debe asignarse al menos un propietario a la sucursal.");
        }

        repository.deleteAllByBranchId(branchId);
        repository.flush();
        repository.saveAll(
                requestedIds.stream()
                        .map(userId -> new BranchUserAssignment(branchId, userId))
                        .toList()
        );
        repository.flush();

        return findAll(branchId);
    }
}
