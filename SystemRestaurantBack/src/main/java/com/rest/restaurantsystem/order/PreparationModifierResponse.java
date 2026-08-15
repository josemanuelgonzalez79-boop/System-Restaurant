package com.rest.restaurantsystem.order;

public record PreparationModifierResponse(
        Long id,
        String groupName,
        String optionName
) {

    static PreparationModifierResponse from(PreparationItemModifier modifier) {
        return new PreparationModifierResponse(
                modifier.getId(),
                modifier.getGroupName(),
                modifier.getOptionName()
        );
    }
}
