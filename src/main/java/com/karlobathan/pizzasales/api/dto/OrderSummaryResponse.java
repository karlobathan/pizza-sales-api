package com.karlobathan.pizzasales.api.dto;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * An order without its items, as listed by the order search.
 *
 * @param id        database id
 * @param orderDate date the order was placed
 * @param orderTime time the order was placed
 */
public record OrderSummaryResponse(
        Long id,
        LocalDate orderDate,
        LocalTime orderTime
) {
}
