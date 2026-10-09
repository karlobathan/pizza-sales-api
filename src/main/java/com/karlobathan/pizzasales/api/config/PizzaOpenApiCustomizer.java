package com.karlobathan.pizzasales.api.config;

import com.karlobathan.pizzasales.api.ApiResources;
import io.swagger.v3.oas.models.OpenAPI;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.stereotype.Component;

/**
 * Registers the error responses of the pizza type and pizza endpoints.
 */
@Component
public class PizzaOpenApiCustomizer implements OpenApiCustomizer {

    // component name prefixes; ApiDocs builds its refs from these
    static final String PIZZA_TYPE = "PizzaType";
    static final String PIZZA      = "Pizza";

    @Override
    public void customise(OpenAPI openApi) {
        ProblemDetailResponses.addByIdResponses(openApi, PIZZA_TYPE, ApiResources.PIZZA_TYPE, ApiResources.PIZZA_TYPES_PATH);
        ProblemDetailResponses.addByIdResponses(openApi, PIZZA, ApiResources.PIZZA, ApiResources.PIZZAS_PATH);
    }
}
