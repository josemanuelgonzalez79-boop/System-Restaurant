package com.rest.restaurantsystem.customer;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CustomerSaveRequest(
        @NotBlank @Size(max = 120) String name,
        @Size(max = 30) String phone,
        @Email @Size(max = 120) String email,
        @Size(max = 500) String preferences,
        @Size(max = 300) String dietaryNotes,
        Long expectedVersion
) { }
