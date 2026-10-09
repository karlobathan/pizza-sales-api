package com.karlobathan.pizzasales.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * An order with its items and totals.
 *
 * @param id            database id
 * @param orderDate     date the order was placed
 * @param orderTime     time the order was placed
 * @param items         the order's lines, ordered by id
 * @param totalQuantity number of pizzas across all lines
 * @param totalPrice    sum of the line totals, in USD
 */
public record OrderResponse(
        Long id,
        LocalDate orderDate,
        LocalTime orderTime,
        List<OrderItemResponse> items,
        int totalQuantity,
        BigDecimal totalPrice
) {
}
