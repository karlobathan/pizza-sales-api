package com.karlobathan.pizzasales.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * A pizza type's new details ({@code PUT}). The code is left out: it can't be changed.
 *
 * @param name        display name
 * @param category    category name; matched case-insensitively, and created if it doesn't exist yet
 * @param ingredients ingredient names, replacing the current ones; matched case-insensitively, and created if new
 */
public record PizzaTypeUpdateRequest(
        @Schema(example = "The Pepperoni Pizza") @NotBlank @Size(max = 100) String name,
        @Schema(example = "Classic") @NotBlank @Size(max = 50) String category,
        @Schema(example = "[\"Mozzarella Cheese\", \"Pepperoni\"]") @NotEmpty List<@NotBlank @Size(max = 100) String> ingredients
) {
}
