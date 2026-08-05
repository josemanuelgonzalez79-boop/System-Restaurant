package com.rest.restaurantsystem.user;

import java.time.Instant;

public record UserResponse(
        Long id,
        String username,
        String fullName,
        UserRole role,
        boolean active,
        boolean mustChangePassword,
        long version,
        Instant createdAt,
        Instant updatedAt
) {

    static UserResponse from(AppUser user) {
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getFullName(),
                user.getRole(),
                user.isActive(),
                user.isMustChangePassword(),
                user.getVersion(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}
