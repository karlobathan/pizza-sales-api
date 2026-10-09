package com.karlobathan.pizzasales.api.repository;

import com.karlobathan.pizzasales.api.domain.PizzaCategory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PizzaCategoryRepository extends JpaRepository<PizzaCategory, Long> {

}
