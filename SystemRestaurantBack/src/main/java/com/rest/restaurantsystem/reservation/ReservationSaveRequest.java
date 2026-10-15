package com.rest.restaurantsystem.reservation;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record ReservationSaveRequest(
        @NotNull Long servicePointId,
        @NotNull LocalDateTime startLocal,
        @NotNull @Min(30) @Max(240) Integer durationMinutes,
        @NotBlank @Size(max = 120) String guestName,
        @Size(max = 30) String guestPhone,
        @NotNull @Min(1) @Max(99) Integer guestCount,
        @Size(max = 500) String notes,
        Long customerId,
        Long expectedVersion
) {
    public ReservationSaveRequest(Long servicePointId, LocalDateTime startLocal, Integer durationMinutes,
                                  String guestName, String guestPhone, Integer guestCount, String notes,
                                  Long expectedVersion) {
        this(servicePointId, startLocal, durationMinutes, guestName, guestPhone, guestCount,
                notes, null, expectedVersion);
    }
}
