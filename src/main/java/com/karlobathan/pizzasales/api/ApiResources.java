package com.karlobathan.pizzasales.api;

/**
 * Names and paths of the API's resources, shared by the controllers' mappings, the services' not found errors
 * and the OpenAPI error examples, so they cannot drift apart. Constants, not an enum, so they can be used in
 * annotations such as {@code @RequestMapping}.
 */
public final class ApiResources {

    /** Resource name used in error messages, e.g. {@code Pizza type with id 99 not found}. */
    public static final String PIZZA_TYPE       = "Pizza type";
    public static final String PIZZA_TYPES_PATH = "/api/pizza-types";

    public static final String PIZZA       = "Pizza";
    public static final String PIZZAS_PATH = "/api/pizzas";

    private ApiResources() {
    }
}
