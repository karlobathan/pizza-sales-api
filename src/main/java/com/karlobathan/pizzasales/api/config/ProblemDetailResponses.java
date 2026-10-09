package com.karlobathan.pizzasales.api.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds the problem detail error responses that the resource customizers register as OpenAPI components.
 * Component names are {@code <resource key><error>}, e.g. {@code PizzaTypeNotFound}.
 */
final class ProblemDetailResponses {

    static final String NOT_FOUND   = "NotFound";
    static final String BAD_REQUEST = "BadRequest";
    static final String WRITE_BAD_REQUEST = "WriteBadRequest";
    static final String CONFLICT = "Conflict";

    // registered by OpenApiConfig
    private static final String PROBLEM_DETAIL_SCHEMA = "#/components/schemas/ProblemDetail";
    private static final long   EXAMPLE_ID            = 99;
    private static final String EXAMPLE_INVALID_ID    = "abc";

    private ProblemDetailResponses() {
    }

    /**
     * Registers {@code <key>NotFound} and {@code <key>BadRequest} for a resource's by-id endpoint.
     *
     * @param key  prefix of the component names, e.g. {@code PizzaType}
     * @param name resource name as used in error messages, from {@code ApiResources}
     * @param path collection path of the resource, from {@code ApiResources}
     */
    static void addByIdResponses(OpenAPI openApi, String key, String name, String path) {
        Components components = components(openApi);
        components.addResponses(key + NOT_FOUND, problemResponse("No %s exists with this id".formatted(name.toLowerCase()),
                // detail matches ResourceNotFoundException's message
                example("Resource not found", 404, "%s with id %d not found".formatted(name, EXAMPLE_ID), path + "/" + EXAMPLE_ID)
        ));
        components.addResponses(key + BAD_REQUEST, problemResponse("The id is not a number",
                // detail matches Spring MVC's message for a path variable that fails type conversion
                example("Bad Request",
                        400,
                        "Failed to convert 'id' with value: '%s'".formatted(EXAMPLE_INVALID_ID),
                        path + "/" + EXAMPLE_INVALID_ID
                )
        ));
    }

    /**
     * Registers a one-off problem detail response, for errors that don't fit the by-id pair.
     */
    static void addResponse(OpenAPI openApi, String component, String description, Map<String, Object> example) {
        components(openApi).addResponses(component, problemResponse(description, example));
    }

    // same fields, in the same order, as the problem details the API actually returns
    static Map<String, Object> example(String title, int status, String detail, String instance) {
        Map<String, Object> example = new LinkedHashMap<>();
        example.put("title", title);
        example.put("status", status);
        example.put("detail", detail);
        example.put("instance", instance);
        return example;
    }

    // a request body that fails validation: the same shape ApiExceptionHandler returns, with the invalid fields
    static Map<String, Object> validationExample(String instance, String field, String message) {
        Map<String, Object> example = example("Invalid request", 400, "The request body has invalid fields", instance);
        example.put("errors", List.of(Map.of("field", field, "message", message)));
        return example;
    }

    // a spec with no schemas yet has no components object, so create it rather than fail
    static Components components(OpenAPI openApi) {
        if (openApi.getComponents() == null) {
            openApi.setComponents(new Components());
        }
        return openApi.getComponents();
    }

    private static ApiResponse problemResponse(String description, Map<String, Object> example) {
        MediaType problemJson = new MediaType().schema(new Schema<>().$ref(PROBLEM_DETAIL_SCHEMA)).example(example);
        return new ApiResponse().description(description)
                .content(new Content().addMediaType(org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                        problemJson
                ));
    }
}
