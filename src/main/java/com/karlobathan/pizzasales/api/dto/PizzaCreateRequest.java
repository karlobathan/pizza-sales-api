package com.karlobathan.pizzasales.api.dto;

import com.karlobathan.pizzasales.api.domain.PizzaSize;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * A new pizza: one size and price of a pizza type ({@code POST}).
 *
 * @param code        unique code, e.g. {@code pepperoni_m}; can't be changed later
 * @param pizzaTypeId database id of the pizza type, as returned by {@code GET /api/pizza-types}
 * @param size        size; a pizza type has at most one pizza per size
 * @param price       price in USD, with at most 2 decimals
 */
public record PizzaCreateRequest(
        @Schema(example = "pepperoni_m")
        @NotBlank @Size(max = MenuCode.MAX_LENGTH) @Pattern(regexp = MenuCode.PATTERN, message = MenuCode.MESSAGE) String code,
        @NotNull Long pizzaTypeId,
        @NotNull PizzaSize size,
        @Schema(example = "12.50") @NotNull @DecimalMin("0.00") @Digits(integer = 4, fraction = 2) BigDecimal price
) {
}
