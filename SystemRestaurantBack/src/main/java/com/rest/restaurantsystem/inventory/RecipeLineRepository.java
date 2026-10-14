package com.rest.restaurantsystem.inventory;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

interface RecipeLineRepository extends JpaRepository<RecipeLine, Long> {

    List<RecipeLine> findByRecipeIdOrderByIdAsc(Long recipeId);

    boolean existsByIngredientId(Long ingredientId);

    @Modifying
    @Query("DELETE FROM RecipeLine line WHERE line.recipeId = :recipeId")
    void deleteByRecipeId(@Param("recipeId") Long recipeId);
}
