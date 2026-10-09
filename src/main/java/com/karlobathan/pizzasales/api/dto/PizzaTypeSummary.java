package com.karlobathan.pizzasales.api.dto;

/**
 * The pizza type a pizza belongs to, without its category and ingredients.
 *
 * @param id   database id
 * @param code source id from the menu data, e.g. {@code bbq_ckn}
 * @param name display name, e.g. {@code The Barbecue Chicken Pizza}
 */
public record PizzaTypeSummary(
        Long id,
        String code,
        String name
) {
}
