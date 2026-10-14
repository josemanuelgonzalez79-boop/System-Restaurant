package com.rest.restaurantsystem.inventory;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

interface RecipeRepository extends JpaRepository<Recipe, Long> {

    Optional<Recipe> findByProductId(Long productId);

    Optional<Recipe> findByModifierOptionId(Long modifierOptionId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT recipe FROM Recipe recipe WHERE recipe.id = :id")
    Optional<Recipe> findByIdForUpdate(@Param("id") Long id);
}
