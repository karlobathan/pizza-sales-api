package com.karlobathan.pizzasales.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * A new pizza type ({@code POST}).
 *
 * @param code        unique code, e.g. {@code pepperoni}; can't be changed later
 * @param name        display name
 * @param category    category name; matched case-insensitively, and created if it doesn't exist yet
 * @param ingredients ingredient names; matched case-insensitively, and created if they don't exist yet
 */
public record PizzaTypeCreateRequest(
        @Schema(example = "pepperoni")
        @NotBlank @Size(max = MenuCode.MAX_LENGTH) @Pattern(regexp = MenuCode.PATTERN, message = MenuCode.MESSAGE) String code,
        @Schema(example = "The Pepperoni Pizza") @NotBlank @Size(max = 100) String name,
        @Schema(example = "Classic") @NotBlank @Size(max = 50) String category,
        @Schema(example = "[\"Mozzarella Cheese\", \"Pepperoni\"]") @NotEmpty List<@NotBlank @Size(max = 100) String> ingredients
) {
}
