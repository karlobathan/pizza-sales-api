package com.karlobathan.pizzasales.api.service;

import com.karlobathan.pizzasales.api.dto.PizzaTypeResponse;
import com.karlobathan.pizzasales.api.exception.ResourceNotFoundException;

import java.util.List;

/**
 * Read access to the menu's pizza types: recipes with their category and ingredients, independent of size and price.
 */
public interface PizzaTypeService {

    /**
     * Returns every pizza type on the menu.
     *
     * @return all pizza types ordered by id, each with its category and ingredients (sorted by name);
     * an empty list if none have been imported
     */
    List<PizzaTypeResponse> findAll();

    /**
     * Returns one pizza type.
     *
     * @param id the pizza type's database id
     * @return the pizza type with its category and ingredients (sorted by name)
     * @throws ResourceNotFoundException if no pizza type has this id
     */
    PizzaTypeResponse findById(Long id);
}
