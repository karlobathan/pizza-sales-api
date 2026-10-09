package com.karlobathan.pizzasales.api.dto;

import com.karlobathan.pizzasales.api.config.ApiDocs;
import io.swagger.v3.oas.annotations.media.Schema;

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
        @Schema(type = "string", format = "date", example = ApiDocs.DATE_EXAMPLE, description = ApiDocs.DATE_DESCRIPTION) LocalDate orderDate,
        @Schema(type = "string", format = "time", example = ApiDocs.TIME_EXAMPLE) LocalTime orderTime
) {
}
