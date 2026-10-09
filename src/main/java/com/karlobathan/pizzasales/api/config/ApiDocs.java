package com.karlobathan.pizzasales.api.config;

import static com.karlobathan.pizzasales.api.config.ProblemDetailResponses.BAD_REQUEST;
import static com.karlobathan.pizzasales.api.config.ProblemDetailResponses.NOT_FOUND;

/**
 * References to the error response components registered by the resource customizers
 * ({@link PizzaOpenApiCustomizer}, {@link OrderOpenApiCustomizer}), for use in {@code @ApiResponse(ref = ...)}
 * so error responses are described once instead of on every endpoint.
 */
public final class ApiDocs {

    private static final String RESPONSES = "#/components/responses/";

    public static final String PIZZA_TYPE_NOT_FOUND   = RESPONSES + PizzaOpenApiCustomizer.PIZZA_TYPE + NOT_FOUND;
    public static final String PIZZA_TYPE_BAD_REQUEST = RESPONSES + PizzaOpenApiCustomizer.PIZZA_TYPE + BAD_REQUEST;

    public static final String PIZZA_NOT_FOUND   = RESPONSES + PizzaOpenApiCustomizer.PIZZA + NOT_FOUND;
    public static final String PIZZA_BAD_REQUEST = RESPONSES + PizzaOpenApiCustomizer.PIZZA + BAD_REQUEST;

    public static final String ORDER_NOT_FOUND          = RESPONSES + OrderOpenApiCustomizer.ORDER + NOT_FOUND;
    public static final String ORDER_BAD_REQUEST        = RESPONSES + OrderOpenApiCustomizer.ORDER + BAD_REQUEST;
    public static final String ORDER_SEARCH_BAD_REQUEST = RESPONSES + OrderOpenApiCustomizer.ORDER_SEARCH_BAD_REQUEST;

    private ApiDocs() {
    }
}
