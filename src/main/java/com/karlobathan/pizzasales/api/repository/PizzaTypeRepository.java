package com.karlobathan.pizzasales.api.repository;

import com.karlobathan.pizzasales.api.domain.PizzaType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Set;

public interface PizzaTypeRepository extends JpaRepository<PizzaType, Long> {

    @Query("select pt.code from PizzaType pt")
    Set<String> findAllCode();
}
