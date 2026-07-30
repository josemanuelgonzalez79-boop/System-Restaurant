package com.rest.restaurantsystem.restaurant;

import java.time.Instant;

public record RestaurantSettingsResponse(
        Long id,
        String businessName,
        String displayName,
        String currencyCode,
        String timezone,
        String primaryColor,
        String secondaryColor,
        String phone,
        String address,
        long version,
        Instant updatedAt
) {

    static RestaurantSettingsResponse from(RestaurantSettings settings) {
        return new RestaurantSettingsResponse(
                settings.getId(),
                settings.getBusinessName(),
                settings.getDisplayName(),
                settings.getCurrencyCode(),
                settings.getTimezone(),
                settings.getPrimaryColor(),
                settings.getSecondaryColor(),
                settings.getPhone(),
                settings.getAddress(),
                settings.getVersion(),
                settings.getUpdatedAt()
        );
    }
}
