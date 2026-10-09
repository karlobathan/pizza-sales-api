package com.karlobathan.pizzasales.api.repository;

import com.karlobathan.pizzasales.api.domain.Pizza;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface PizzaRepository extends JpaRepository<Pizza, Long> {

    @Query("select p.code from Pizza p")
    Set<String> findAllCode();

    // fetches the pizza type in the same query, as it is lazy and the API maps it
    @EntityGraph(attributePaths = "pizzaType")
    List<Pizza> findAllByOrderByIdAsc();

    @EntityGraph(attributePaths = "pizzaType")
    Optional<Pizza> findWithPizzaTypeById(Long id);

    @EntityGraph(attributePaths = "pizzaType")
    List<Pizza> findWithPizzaTypeByIdIn(Collection<Long> ids);
}
