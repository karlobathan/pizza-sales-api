package com.karlobathan.pizzasales.api.dto;

/**
 * Format of pizza and pizza type codes, e.g. {@code pepperoni} or {@code pepperoni_m}: the natural keys the CSV
 * import matches on. Shared by the request validation and the OpenAPI examples.
 */
public final class MenuCode {

    public static final String PATTERN    = "^[a-z0-9_]+$";
    public static final String MESSAGE    = "must contain only lowercase letters, digits and underscores";
    public static final int    MAX_LENGTH = 50;

    private MenuCode() {
    }
}
