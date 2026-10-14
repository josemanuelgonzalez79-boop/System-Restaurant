package com.rest.restaurantsystem.inventory;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

interface IngredientRepository extends JpaRepository<Ingredient, Long> {

    List<Ingredient> findAllByOrderByNameAsc();

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT ingredient FROM Ingredient ingredient WHERE ingredient.id = :id")
    Optional<Ingredient> findByIdForUpdate(@Param("id") Long id);
}
