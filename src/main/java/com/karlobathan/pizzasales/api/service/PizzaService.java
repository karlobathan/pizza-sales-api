package com.karlobathan.pizzasales.api.service;

import com.karlobathan.pizzasales.api.dto.PizzaResponse;
import com.karlobathan.pizzasales.api.exception.ResourceNotFoundException;

import java.util.List;

/**
 * Read access to sellable pizzas: each size and price variant of a pizza type.
 */
public interface PizzaService {

    /**
     * Returns every pizza.
     *
     * @return all pizzas ordered by id, each with a summary of its pizza type; an empty list if none have been imported
     */
    List<PizzaResponse> findAll();

    /**
     * Returns one pizza.
     *
     * @param id the pizza's database id
     * @return the pizza with its size, price and a summary of its pizza type
     * @throws ResourceNotFoundException if no pizza has this id
     */
    PizzaResponse findById(Long id);
}
