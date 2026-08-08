package com.rest.restaurantsystem.structure;

import com.rest.restaurantsystem.user.UserResponse;
import com.rest.restaurantsystem.user.UserRole;

public record BranchAssignmentUserResponse(
        Long userId,
        String username,
        String fullName,
        UserRole role,
        boolean active,
        boolean assigned
) {

    static BranchAssignmentUserResponse from(UserResponse user, boolean assigned) {
        return new BranchAssignmentUserResponse(
                user.id(),
                user.username(),
                user.fullName(),
                user.role(),
                user.active(),
                assigned
        );
    }
}
