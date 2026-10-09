package com.karlobathan.pizzasales.api.service;

import com.karlobathan.pizzasales.api.dto.PizzaCreateRequest;
import com.karlobathan.pizzasales.api.dto.PizzaResponse;
import com.karlobathan.pizzasales.api.dto.PizzaUpdateRequest;
import com.karlobathan.pizzasales.api.exception.ConflictException;
import com.karlobathan.pizzasales.api.exception.InvalidRequestException;
import com.karlobathan.pizzasales.api.exception.ResourceNotFoundException;

import java.util.List;

/**
 * Access to sellable pizzas: each size and price variant of a pizza type. Deleted pizzas are soft-deleted: no method
 * returns or changes them, but orders placed with them still show them.
 */
public interface PizzaService {

    /**
     * Returns every pizza.
     *
     * @return all pizzas not deleted, ordered by id, each with a summary of its pizza type; an empty list if there
     * are none
     */
    List<PizzaResponse> findAll();

    /**
     * Returns one pizza.
     *
     * @param id the pizza's database id
     * @return the pizza with its size, price and a summary of its pizza type
     * @throws ResourceNotFoundException if no pizza has this id, or it is deleted
     */
    PizzaResponse findById(Long id);

    /**
     * Creates a pizza.
     *
     * @param request the pizza's code, pizza type, size and price
     * @return the created pizza with its new id
     * @throws ConflictException       if the code is already used by another pizza (deleted ones included), or the
     *                                 pizza type already has a pizza of this size
     * @throws InvalidRequestException if the pizza type doesn't exist or is deleted
     */
    PizzaResponse create(PizzaCreateRequest request);

    /**
     * Replaces a pizza's pizza type, size and price; the code stays. Orders show their totals at the current price,
     * so a price change also changes the totals of past orders with this pizza.
     *
     * @param id      the pizza's database id
     * @param request the new pizza type, size and price
     * @return the updated pizza
     * @throws ResourceNotFoundException if no pizza has this id, or it is deleted
     * @throws ConflictException         if the pizza type already has another pizza of this size
     * @throws InvalidRequestException   if the pizza type doesn't exist or is deleted
     */
    PizzaResponse replace(Long id, PizzaUpdateRequest request);

    /**
     * Soft-deletes a pizza: from then on no menu endpoint returns it and new orders can't use it, but orders already
     * placed with it still show it. It stays in the database, so re-running the import doesn't bring it back.
     *
     * @param id the pizza's database id
     * @throws ResourceNotFoundException if no pizza has this id, or it is already deleted
     */
    void delete(Long id);
}
