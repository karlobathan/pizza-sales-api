package com.karlobathan.pizzasales.api.repository;

import com.karlobathan.pizzasales.api.domain.PizzaIngredient;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PizzaIngredientRepository extends JpaRepository<PizzaIngredient, Long> {

}
