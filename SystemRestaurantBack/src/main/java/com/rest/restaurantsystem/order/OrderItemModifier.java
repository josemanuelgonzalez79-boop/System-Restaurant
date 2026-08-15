package com.rest.restaurantsystem.order;

import com.rest.restaurantsystem.catalog.modifier.ModifierOptionResponse;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "order_item_modifiers")
class OrderItemModifier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_item_id", nullable = false)
    private Long orderItemId;

    @Column(name = "modifier_option_id", nullable = false)
    private Long modifierOptionId;

    @Column(name = "group_name", nullable = false, length = 100)
    private String groupName;

    @Column(name = "option_name", nullable = false, length = 100)
    private String optionName;

    @Column(name = "price_delta", nullable = false, precision = 12, scale = 2)
    private BigDecimal priceDelta;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected OrderItemModifier() {
    }

    OrderItemModifier(Long orderItemId, ModifierOptionResponse option) {
        this.orderItemId = orderItemId;
        modifierOptionId = option.id();
        groupName = option.groupName();
        optionName = option.name();
        priceDelta = option.priceDelta();
    }

    Long getId() {
        return id;
    }

    Long getOrderItemId() {
        return orderItemId;
    }

    Long getModifierOptionId() {
        return modifierOptionId;
    }

    String getGroupName() {
        return groupName;
    }

    String getOptionName() {
        return optionName;
    }

    BigDecimal getPriceDelta() {
        return priceDelta;
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }
}
