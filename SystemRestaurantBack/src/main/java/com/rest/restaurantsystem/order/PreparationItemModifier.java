package com.rest.restaurantsystem.order;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "preparation_item_modifiers")
class PreparationItemModifier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "preparation_item_id", nullable = false)
    private Long preparationItemId;

    @Column(name = "group_name", nullable = false, length = 100)
    private String groupName;

    @Column(name = "option_name", nullable = false, length = 100)
    private String optionName;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    protected PreparationItemModifier() {
    }

    PreparationItemModifier(
            Long preparationItemId,
            OrderItemModifier modifier,
            int sortOrder
    ) {
        this.preparationItemId = preparationItemId;
        groupName = modifier.getGroupName();
        optionName = modifier.getOptionName();
        this.sortOrder = sortOrder;
    }

    Long getId() {
        return id;
    }

    Long getPreparationItemId() {
        return preparationItemId;
    }

    String getGroupName() {
        return groupName;
    }

    String getOptionName() {
        return optionName;
    }

    int getSortOrder() {
        return sortOrder;
    }
}
