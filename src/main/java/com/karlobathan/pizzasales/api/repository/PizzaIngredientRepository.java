package com.karlobathan.pizzasales.api.repository;

import com.karlobathan.pizzasales.api.domain.PizzaIngredient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;

public interface PizzaIngredientRepository extends JpaRepository<PizzaIngredient, Long> {

    // case-insensitive match on many names at once; callers pass the names already lower-cased
    @Query("select i from PizzaIngredient i where lower(i.name) in :lowerCaseNames")
    List<PizzaIngredient> findByLowerCaseNameIn(Collection<String> lowerCaseNames);
}
