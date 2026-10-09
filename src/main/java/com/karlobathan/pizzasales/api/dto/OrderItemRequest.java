package com.karlobathan.pizzasales.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * One line of an order being created or replaced.
 *
 * @param pizzaId  database id of the pizza, as returned by {@code GET /api/pizzas}
 * @param quantity how many of this pizza; at least 1
 */
public record OrderItemRequest(
        @NotNull Long pizzaId,
        @NotNull @Positive Integer quantity
) {
}
