package com.rest.restaurantsystem.order;

import com.rest.restaurantsystem.promotion.DiscountType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "order_discounts")
class OrderDiscount {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "operation_id", nullable = false, unique = true, updatable = false)
    private UUID operationId;
    @Column(name = "order_id", nullable = false, updatable = false)
    private Long orderId;
    @Column(name = "promotion_id", updatable = false)
    private Long promotionId;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12, updatable = false)
    private OrderDiscountSource source;
    @Column(name = "code_snapshot", length = 24, updatable = false)
    private String codeSnapshot;
    @Column(name = "name_snapshot", nullable = false, length = 100, updatable = false)
    private String nameSnapshot;
    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type", nullable = false, length = 12, updatable = false)
    private DiscountType discountType;
    @Column(name = "discount_value", nullable = false, precision = 12, scale = 2, updatable = false)
    private BigDecimal discountValue;
    @Column(name = "base_subtotal", nullable = false, precision = 12, scale = 2, updatable = false)
    private BigDecimal baseSubtotal;
    @Column(nullable = false, precision = 12, scale = 2, updatable = false)
    private BigDecimal amount;
    @Column(length = 250, updatable = false)
    private String reason;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private OrderDiscountStatus status;
    @Column(name = "applied_by_user_id", nullable = false, updatable = false)
    private Long appliedByUserId;
    @Column(name = "applied_at", nullable = false, updatable = false)
    private Instant appliedAt;
    @Column(name = "voided_by_user_id")
    private Long voidedByUserId;
    @Column(name = "voided_at")
    private Instant voidedAt;
    @Column(name = "void_reason", length = 250)
    private String voidReason;

    protected OrderDiscount() { }
    OrderDiscount(UUID operationId, Long orderId, Long promotionId,
                  OrderDiscountSource source, String codeSnapshot, String nameSnapshot,
                  DiscountType discountType, BigDecimal discountValue, BigDecimal baseSubtotal,
                  BigDecimal amount, String reason, Long appliedByUserId) {
        this.operationId = operationId;
        this.orderId = orderId;
        this.promotionId = promotionId;
        this.source = source;
        this.codeSnapshot = codeSnapshot;
        this.nameSnapshot = nameSnapshot;
        this.discountType = discountType;
        this.discountValue = discountValue;
        this.baseSubtotal = baseSubtotal;
        this.amount = amount;
        this.reason = reason;
        this.appliedByUserId = appliedByUserId;
        status = OrderDiscountStatus.ACTIVE;
    }

    void voidDiscount(Long userId, String reason) {
        status = OrderDiscountStatus.VOIDED;
        voidedByUserId = userId;
        voidedAt = Instant.now();
        voidReason = reason.trim();
    }

    @PrePersist void onCreate() { appliedAt = Instant.now(); }
    Long getId() { return id; }
    Long getOrderId() { return orderId; }
    UUID getOperationId() { return operationId; }
    Long getPromotionId() { return promotionId; }
    OrderDiscountSource getSource() { return source; }
    String getCodeSnapshot() { return codeSnapshot; }
    String getNameSnapshot() { return nameSnapshot; }
    DiscountType getDiscountType() { return discountType; }
    BigDecimal getDiscountValue() { return discountValue; }
    BigDecimal getBaseSubtotal() { return baseSubtotal; }
    BigDecimal getAmount() { return amount; }
    String getReason() { return reason; }
    OrderDiscountStatus getStatus() { return status; }
    Long getAppliedByUserId() { return appliedByUserId; }
    Instant getAppliedAt() { return appliedAt; }
    Long getVoidedByUserId() { return voidedByUserId; }
    Instant getVoidedAt() { return voidedAt; }
    String getVoidReason() { return voidReason; }
}
