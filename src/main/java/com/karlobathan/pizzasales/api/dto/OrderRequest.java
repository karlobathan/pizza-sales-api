package com.karlobathan.pizzasales.api.dto;

import com.karlobathan.pizzasales.api.config.ApiDocs;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * A whole order, for creating one.
 *
 * @param orderDate date the order was placed
 * @param orderTime time the order was placed
 * @param items     the order's lines; at least one
 */
public record OrderRequest(
        @NotNull @Schema(type = "string", format = "date", example = ApiDocs.DATE_EXAMPLE, description = ApiDocs.DATE_DESCRIPTION) LocalDate orderDate,
        @NotNull @Schema(type = "string", format = "time", example = ApiDocs.TIME_EXAMPLE) LocalTime orderTime,
        @NotEmpty List<@NotNull @Valid OrderItemRequest> items
) {
}
