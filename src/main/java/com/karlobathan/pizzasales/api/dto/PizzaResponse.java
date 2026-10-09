package com.karlobathan.pizzasales.api.dto;

import com.karlobathan.pizzasales.api.domain.PizzaSize;

import java.math.BigDecimal;

/**
 * A sellable pizza: one size and price of a pizza type.
 *
 * @param id        database id
 * @param code      source id from the menu data, e.g. {@code bbq_ckn_s}
 * @param size      pizza size
 * @param price     price with no unit
 * @param pizzaType the pizza type this is a size of
 */
public record PizzaResponse(
        Long id,
        String code,
        PizzaSize size,
        BigDecimal price,
        PizzaTypeSummary pizzaType
) {
}
