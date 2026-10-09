package com.karlobathan.pizzasales.api.repository;

import com.karlobathan.pizzasales.api.domain.PizzaType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface PizzaTypeRepository extends JpaRepository<PizzaType, Long> {

    @Query("select pt.code from PizzaType pt")
    Set<String> findAllCode();

    // fetches category and ingredients in the same query, as both are lazy and the API maps them
    @Query("""
            select distinct pt from PizzaType pt
            join fetch pt.pizzaCategory
            left join fetch pt.pizzaIngredients
            order by pt.id
            """)
    List<PizzaType> findAllWithDetails();

    @EntityGraph(attributePaths = {"pizzaCategory", "pizzaIngredients"})
    Optional<PizzaType> findWithDetailsById(Long id);
}
