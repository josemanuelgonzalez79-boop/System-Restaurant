package com.rest.restaurantsystem.order;

import com.rest.restaurantsystem.structure.BranchAssignmentUserResponse;
import com.rest.restaurantsystem.user.UserRole;

public record OrderOperatorResponse(
        Long id,
        String username,
        String fullName,
        UserRole role
) {

    static OrderOperatorResponse from(BranchAssignmentUserResponse user) {
        return new OrderOperatorResponse(
                user.userId(),
                user.username(),
                user.fullName(),
                user.role()
        );
    }
}
