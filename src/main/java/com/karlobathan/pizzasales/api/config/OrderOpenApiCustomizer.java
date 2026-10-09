package com.karlobathan.pizzasales.api.config;

import com.karlobathan.pizzasales.api.ApiResources;
import com.karlobathan.pizzasales.api.exception.InvalidRequestException;
import io.swagger.v3.oas.models.OpenAPI;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

/**
 * Registers the error responses of the order endpoints.
 */
@Component
public class OrderOpenApiCustomizer implements OpenApiCustomizer {

    // component name prefix; ApiDocs builds its refs from these
    static final String ORDER = "Order";

    // the order search's 400 is about its query parameters, not an id, so it has its own component
    static final String ORDER_SEARCH_BAD_REQUEST = "OrderSearchBadRequest";

    // creating an order's 400 is about the request body, so it has its own component too
    static final String ORDER_CREATE_BAD_REQUEST = "OrderCreateBadRequest";

    @Override
    public void customise(OpenAPI openApi) {
        ProblemDetailResponses.addByIdResponses(openApi, ORDER, ApiResources.ORDER, ApiResources.ORDERS_PATH);

        ProblemDetailResponses.addResponse(openApi,
                ORDER_SEARCH_BAD_REQUEST,
                "Invalid search parameters: 'from' is after 'to', a date is not yyyy-MM-dd, "
                        + "'page' is negative, or 'size' is not between 1 and 100",
                ProblemDetailResponses.example("Invalid request",
                        400,
                        // built by the same factory the service throws, so the example can't drift from it
                        InvalidRequestException.fromAfterTo(LocalDate.of(2015, 12, 31), LocalDate.of(2015, 1, 1))
                                .getMessage(),
                        ApiResources.ORDERS_PATH
                )
        );

        ProblemDetailResponses.addResponse(openApi,
                ORDER_CREATE_BAD_REQUEST,
                "Invalid order: an item references a pizza that doesn't exist, a required field is missing, "
                        + "items is empty, a quantity is below 1, or the body is not valid JSON. "
                        + "Invalid fields are listed in 'errors'.",
                ProblemDetailResponses.example("Invalid request",
                        400,
                        // built by the same factory the service throws, so the example can't drift from it
                        InvalidRequestException.unknownPizzas(List.of(99L)).getMessage(),
                        ApiResources.ORDERS_PATH
                )
        );
    }
}
