package com.rest.restaurantsystem.business;

import java.time.Instant;

public record BusinessSettingsResponse(
        Long id,
        String businessName,
        String displayName,
        BusinessType businessType,
        String currencyCode,
        String timezone,
        String primaryColor,
        String secondaryColor,
        String phone,
        String address,
        long version,
        Instant updatedAt
) {

    static BusinessSettingsResponse from(BusinessSettings settings) {
        return new BusinessSettingsResponse(
                settings.getId(),
                settings.getBusinessName(),
                settings.getDisplayName(),
                settings.getBusinessType(),
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
