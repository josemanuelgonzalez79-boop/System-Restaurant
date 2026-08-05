package com.rest.restaurantsystem.business;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;

@Entity
@Table(name = "business_settings")
class BusinessSettings {

    static final long SINGLETON_ID = 1L;

    @Id
    private Long id;

    @Column(name = "business_name", nullable = false, length = 120)
    private String businessName;

    @Column(name = "display_name", nullable = false, length = 80)
    private String displayName;

    @Enumerated(EnumType.STRING)
    @Column(name = "business_type", nullable = false, length = 30)
    private BusinessType businessType;

    @Column(name = "currency_code", nullable = false, length = 3)
    private String currencyCode;

    @Column(nullable = false, length = 60)
    private String timezone;

    @Column(name = "primary_color", nullable = false, length = 7)
    private String primaryColor;

    @Column(name = "secondary_color", nullable = false, length = 7)
    private String secondaryColor;

    @Column(length = 30)
    private String phone;

    @Column(length = 250)
    private String address;

    @Version
    private long version;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected BusinessSettings() {
    }

    void update(BusinessSettingsRequest request) {
        businessName = request.businessName().trim();
        displayName = request.displayName().trim();
        businessType = request.businessType();
        currencyCode = request.currencyCode().trim().toUpperCase();
        timezone = request.timezone().trim();
        primaryColor = request.primaryColor().trim().toUpperCase();
        secondaryColor = request.secondaryColor().trim().toUpperCase();
        phone = normalizeOptional(request.phone());
        address = normalizeOptional(request.address());
        updatedAt = Instant.now();
    }

    Long getId() {
        return id;
    }

    String getBusinessName() {
        return businessName;
    }

    String getDisplayName() {
        return displayName;
    }

    BusinessType getBusinessType() {
        return businessType;
    }

    String getCurrencyCode() {
        return currencyCode;
    }

    String getTimezone() {
        return timezone;
    }

    String getPrimaryColor() {
        return primaryColor;
    }

    String getSecondaryColor() {
        return secondaryColor;
    }

    String getPhone() {
        return phone;
    }

    String getAddress() {
        return address;
    }

    long getVersion() {
        return version;
    }

    Instant getUpdatedAt() {
        return updatedAt;
    }

    private String normalizeOptional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
