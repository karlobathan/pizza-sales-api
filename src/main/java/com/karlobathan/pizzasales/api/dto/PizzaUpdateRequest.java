package com.karlobathan.pizzasales.api.dto;

import com.karlobathan.pizzasales.api.domain.PizzaSize;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * A pizza's new details ({@code PUT}). The code is left out: it can't be changed.
 *
 * @param pizzaTypeId database id of the pizza type, as returned by {@code GET /api/pizza-types}
 * @param size        size; a pizza type has at most one pizza per size
 * @param price       price in USD, with at most 2 decimals; orders show their totals at the current price
 */
public record PizzaUpdateRequest(
        @NotNull Long pizzaTypeId,
        @NotNull PizzaSize size,
        @Schema(example = "12.50") @NotNull @DecimalMin("0.00") @Digits(integer = 4, fraction = 2) BigDecimal price
) {
}
