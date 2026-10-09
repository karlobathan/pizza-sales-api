package com.karlobathan.pizzasales.api.config;

/**
 * References to the shared OpenAPI components registered by {@link OpenApiConfig}, for use in
 * {@code @ApiResponse(ref = ...)} so error responses are described once instead of on every endpoint.
 */
public final class ApiDocs {

    private static final String RESPONSES = "#/components/responses/";

    public static final String PIZZA_TYPE_NOT_FOUND   = RESPONSES + OpenApiConfig.PIZZA_TYPE + OpenApiConfig.NOT_FOUND;
    public static final String PIZZA_TYPE_BAD_REQUEST = RESPONSES + OpenApiConfig.PIZZA_TYPE + OpenApiConfig.BAD_REQUEST;

    public static final String PIZZA_NOT_FOUND   = RESPONSES + OpenApiConfig.PIZZA + OpenApiConfig.NOT_FOUND;
    public static final String PIZZA_BAD_REQUEST = RESPONSES + OpenApiConfig.PIZZA + OpenApiConfig.BAD_REQUEST;

    private ApiDocs() {
    }
}
