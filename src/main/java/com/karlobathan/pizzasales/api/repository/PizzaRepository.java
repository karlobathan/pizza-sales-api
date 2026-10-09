package com.karlobathan.pizzasales.api.repository;

import com.karlobathan.pizzasales.api.domain.Pizza;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Set;

public interface PizzaRepository extends JpaRepository<Pizza, Long> {

    @Query("select p.code from Pizza p")
    Set<String> findAllCode();
}
