package com.karlobathan.pizzasales.api.dto;

import com.karlobathan.pizzasales.api.config.ApiDocs;
import io.swagger.v3.oas.annotations.media.Schema;

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
        @Schema(type = "string", format = "date", example = ApiDocs.DATE_EXAMPLE, description = ApiDocs.DATE_DESCRIPTION) LocalDate orderDate,
        @Schema(type = "string", format = "time", example = ApiDocs.TIME_EXAMPLE) LocalTime orderTime,
        List<OrderItemResponse> items,
        int totalQuantity,
        BigDecimal totalPrice
) {
}
