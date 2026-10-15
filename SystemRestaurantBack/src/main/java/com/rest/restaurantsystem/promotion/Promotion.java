package com.rest.restaurantsystem.promotion;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "promotions")
class Promotion {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 24)
    private String code;
    @Column(nullable = false, length = 100)
    private String name;
    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type", nullable = false, length = 12)
    private DiscountType discountType;
    @Column(name = "discount_value", nullable = false, precision = 12, scale = 2)
    private BigDecimal discountValue;
    @Column(name = "minimum_subtotal", nullable = false, precision = 12, scale = 2)
    private BigDecimal minimumSubtotal;
    @Column(name = "starts_on", nullable = false)
    private LocalDate startsOn;
    @Column(name = "ends_on", nullable = false)
    private LocalDate endsOn;
    @Column(nullable = false)
    private boolean active = true;
    @Version
    private long version;
    @Column(name = "created_by_username", nullable = false, updatable = false, length = 80)
    private String createdByUsername;
    @Column(name = "updated_by_username", nullable = false, length = 80)
    private String updatedByUsername;
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Promotion() { }
    Promotion(PromotionSaveRequest request, String code, String username) {
        createdByUsername = username;
        update(request, code, username);
    }
    void update(PromotionSaveRequest request, String code, String username) {
        this.code = code;
        name = request.name().trim();
        discountType = request.discountType();
        discountValue = request.discountValue();
        minimumSubtotal = request.minimumSubtotal();
        startsOn = request.startsOn();
        endsOn = request.endsOn();
        active = request.active();
        updatedByUsername = username;
    }
    @PrePersist void onCreate() { createdAt = Instant.now(); updatedAt = createdAt; }
    @PreUpdate void onUpdate() { updatedAt = Instant.now(); }
    Long getId() { return id; }
    String getCode() { return code; }
    String getName() { return name; }
    DiscountType getDiscountType() { return discountType; }
    BigDecimal getDiscountValue() { return discountValue; }
    BigDecimal getMinimumSubtotal() { return minimumSubtotal; }
    LocalDate getStartsOn() { return startsOn; }
    LocalDate getEndsOn() { return endsOn; }
    boolean isActive() { return active; }
    long getVersion() { return version; }
    String getCreatedByUsername() { return createdByUsername; }
    String getUpdatedByUsername() { return updatedByUsername; }
    Instant getCreatedAt() { return createdAt; }
    Instant getUpdatedAt() { return updatedAt; }
}
