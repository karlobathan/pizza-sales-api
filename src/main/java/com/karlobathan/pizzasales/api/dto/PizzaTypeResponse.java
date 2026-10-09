package com.karlobathan.pizzasales.api.dto;

import java.util.List;

/**
 * A pizza type on the menu: its recipe, independent of size and price.
 *
 * @param id          database id
 * @param code        source id from the menu data, e.g. {@code bbq_ckn}
 * @param name        display name, e.g. {@code The Barbecue Chicken Pizza}
 * @param category    category name, e.g. {@code Chicken}
 * @param ingredients ingredient names, sorted alphabetically
 */
public record PizzaTypeResponse(
        Long id,
        String code,
        String name,
        String category,
        List<String> ingredients
) {
}
