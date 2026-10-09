package com.karlobathan.pizzasales.api.config;

import com.karlobathan.pizzasales.api.ApiResources;
import com.karlobathan.pizzasales.api.dto.MenuCode;
import com.karlobathan.pizzasales.api.exception.ConflictException;
import com.karlobathan.pizzasales.api.exception.InvalidRequestException;
import io.swagger.v3.oas.models.OpenAPI;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.stereotype.Component;

import static com.karlobathan.pizzasales.api.config.ProblemDetailResponses.CONFLICT;
import static com.karlobathan.pizzasales.api.config.ProblemDetailResponses.WRITE_BAD_REQUEST;

/**
 * Registers the error responses of the pizza type and pizza endpoints: per resource, the by-id 404 and 400, the
 * 400 for an invalid request body, and the 409 for a conflict with existing data.
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

        // examples are built from the same factories and constants the API uses, so they can't drift from it
        ProblemDetailResponses.addResponse(openApi,
                PIZZA_TYPE + WRITE_BAD_REQUEST,
                "Invalid pizza type: a required field is missing or too long, the code has characters other than "
                        + "lowercase letters, digits and underscores, ingredients is empty, the body is not valid JSON, "
                        + "or (when replacing) the id is not a number. Invalid fields are listed in 'errors'.",
                ProblemDetailResponses.validationExample(ApiResources.PIZZA_TYPES_PATH, "code", MenuCode.MESSAGE)
        );
        ProblemDetailResponses.addResponse(openApi,
                PIZZA_TYPE + CONFLICT,
                "The code is already used by another pizza type (deleted ones included), "
                        + "or the pizza type being deleted still has pizzas",
                ProblemDetailResponses.example("Conflict",
                        409,
                        ConflictException.codeTaken(ApiResources.PIZZA_TYPE, "pepperoni").getMessage(),
                        ApiResources.PIZZA_TYPES_PATH
                )
        );
        ProblemDetailResponses.addResponse(openApi,
                PIZZA + WRITE_BAD_REQUEST,
                "Invalid pizza: the pizza type doesn't exist or is deleted, a required field is missing, the code has "
                        + "characters other than lowercase letters, digits and underscores, the size is not S, M, L, XL "
                        + "or XXL, the price is negative or has more than 2 decimals, the body is not valid JSON, "
                        + "or (when replacing) the id is not a number. Invalid fields are listed in 'errors'.",
                ProblemDetailResponses.example("Invalid request",
                        400,
                        InvalidRequestException.unknownPizzaType(99L).getMessage(),
                        ApiResources.PIZZAS_PATH
                )
        );
        ProblemDetailResponses.addResponse(openApi,
                PIZZA + CONFLICT,
                "The code is already used by another pizza (deleted ones included), "
                        + "or the pizza type already has a pizza of this size",
                ProblemDetailResponses.example("Conflict",
                        409,
                        ConflictException.codeTaken(ApiResources.PIZZA, "pepperoni_xl").getMessage(),
                        ApiResources.PIZZAS_PATH
                )
        );
    }
}
