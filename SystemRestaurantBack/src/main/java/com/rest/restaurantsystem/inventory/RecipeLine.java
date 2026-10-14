package com.rest.restaurantsystem.inventory;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(name = "inventory_recipe_lines")
class RecipeLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "recipe_id", nullable = false)
    private Long recipeId;

    @Column(name = "ingredient_id", nullable = false)
    private Long ingredientId;

    @Column(nullable = false, precision = 14, scale = 3)
    private BigDecimal quantity;

    protected RecipeLine() {
    }

    RecipeLine(Long recipeId, Long ingredientId, BigDecimal quantity) {
        this.recipeId = recipeId;
        this.ingredientId = ingredientId;
        this.quantity = quantity;
    }

    Long getIngredientId() {
        return ingredientId;
    }

    Long getRecipeId() {
        return recipeId;
    }

    BigDecimal getQuantity() {
        return quantity;
    }
}
