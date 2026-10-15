package com.rest.restaurantsystem.customer;

import java.time.Instant;

public record CustomerResponse(
        Long id, String name, String phone, String email, String preferences,
        String dietaryNotes, boolean active, long version, String createdByUsername,
        String updatedByUsername, Instant createdAt, Instant updatedAt
) {
    static CustomerResponse from(Customer customer) {
        return new CustomerResponse(customer.getId(), customer.getName(), customer.getPhone(),
                customer.getEmail(), customer.getPreferences(), customer.getDietaryNotes(),
                customer.isActive(), customer.getVersion(), customer.getCreatedByUsername(),
                customer.getUpdatedByUsername(), customer.getCreatedAt(), customer.getUpdatedAt());
    }
}
