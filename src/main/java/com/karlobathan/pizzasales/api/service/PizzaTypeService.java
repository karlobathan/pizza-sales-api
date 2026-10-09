package com.karlobathan.pizzasales.api.service;

import com.karlobathan.pizzasales.api.dto.PizzaTypeCreateRequest;
import com.karlobathan.pizzasales.api.dto.PizzaTypeResponse;
import com.karlobathan.pizzasales.api.dto.PizzaTypeUpdateRequest;
import com.karlobathan.pizzasales.api.exception.ConflictException;
import com.karlobathan.pizzasales.api.exception.ResourceNotFoundException;

import java.util.List;

/**
 * Access to the menu's pizza types: recipes with their category and ingredients, independent of size and price.
 * Deleted pizza types are soft-deleted: no method returns or changes them.
 */
public interface PizzaTypeService {

    /**
     * Returns every pizza type on the menu.
     *
     * @return all pizza types not deleted, ordered by id, each with its category and ingredients (sorted by name);
     * an empty list if there are none
     */
    List<PizzaTypeResponse> findAll();

    /**
     * Returns one pizza type.
     *
     * @param id the pizza type's database id
     * @return the pizza type with its category and ingredients (sorted by name)
     * @throws ResourceNotFoundException if no pizza type has this id, or it is deleted
     */
    PizzaTypeResponse findById(Long id);

    /**
     * Creates a pizza type. Its category and ingredients are matched by name, ignoring case, and created if they
     * don't exist yet; repeated ingredient names are stored once.
     *
     * @param request the pizza type's code, name, category and ingredients
     * @return the created pizza type with its new id
     * @throws ConflictException if the code is already used by another pizza type, deleted ones included
     */
    PizzaTypeResponse create(PizzaTypeCreateRequest request);

    /**
     * Replaces a pizza type's name, category and ingredients; the code stays. Category and ingredients are matched
     * and created as in {@link #create}.
     *
     * @param id      the pizza type's database id
     * @param request the new name, category and ingredients
     * @return the updated pizza type
     * @throws ResourceNotFoundException if no pizza type has this id, or it is deleted
     */
    PizzaTypeResponse replace(Long id, PizzaTypeUpdateRequest request);

    /**
     * Soft-deletes a pizza type: from then on no menu endpoint returns it and no new pizza can use it. It stays in
     * the database, so re-running the import doesn't bring it back.
     *
     * @param id the pizza type's database id
     * @throws ResourceNotFoundException if no pizza type has this id, or it is already deleted
     * @throws ConflictException         if it still has pizzas that aren't deleted
     */
    void delete(Long id);
}
