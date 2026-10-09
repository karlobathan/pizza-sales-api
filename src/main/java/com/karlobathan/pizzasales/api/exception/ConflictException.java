package com.karlobathan.pizzasales.api.exception;

import com.karlobathan.pizzasales.api.domain.PizzaSize;

/**
 * Thrown when a request would clash with existing data; rendered as a 409 by {@link ApiExceptionHandler}.
 * The OpenAPI examples use these factories too, so docs and API agree.
 */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }

    /**
     * For a code already used by another row, deleted ones included: the CSV import matches on codes.
     */
    public static ConflictException codeTaken(String resource, String code) {
        return new ConflictException("%s code '%s' is already in use".formatted(resource, code));
    }

    public static ConflictException sizeTaken(String pizzaTypeCode, PizzaSize size) {
        return new ConflictException("Pizza type '%s' already has a size %s pizza".formatted(pizzaTypeCode, size));
    }

    public static ConflictException stillHasPizzas(Long pizzaTypeId) {
        return new ConflictException("Pizza type with id %d still has pizzas; delete them first".formatted(pizzaTypeId));
    }
}
