package com.karlobathan.pizzasales.api.dto;

import java.math.BigDecimal;

/**
 * One line of an order.
 *
 * @param id        database id
 * @param pizza     the pizza ordered, with its size and unit price
 * @param quantity  how many of this pizza
 * @param lineTotal unit price times quantity
 */
public record OrderItemResponse(
        Long id,
        PizzaResponse pizza,
        Integer quantity,
        BigDecimal lineTotal
) {
}
