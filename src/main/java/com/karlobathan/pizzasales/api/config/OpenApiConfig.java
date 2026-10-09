package com.karlobathan.pizzasales.api.config;

import com.karlobathan.pizzasales.api.ApiResources;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.ProblemDetail;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Registers the API's error responses as OpenAPI components, one 404 and one 400 per resource so each example
 * names its own resource and path. Endpoints point to them with {@code @ApiResponse(ref = ApiDocs.PIZZA_NOT_FOUND)}
 * and so on, so each error's schema and example live here only.
 */
@Configuration
public class OpenApiConfig {

    // component names are <resource key><error>, e.g. PizzaTypeNotFound; ApiDocs builds its refs from these
    static final String PIZZA_TYPE  = "PizzaType";
    static final String PIZZA       = "Pizza";
    static final String NOT_FOUND   = "NotFound";
    static final String BAD_REQUEST = "BadRequest";

    /**
     * Resources with by-id endpoints. To add one: add an entry here and its constants in {@link ApiResources}
     * and {@link ApiDocs}.
     */
    private static final List<DocumentedResource> RESOURCES = List.of(
            new DocumentedResource(PIZZA_TYPE, ApiResources.PIZZA_TYPE, ApiResources.PIZZA_TYPES_PATH),
            new DocumentedResource(PIZZA, ApiResources.PIZZA, ApiResources.PIZZAS_PATH)
    );

    private static final String PROBLEM_DETAIL_SCHEMA = "#/components/schemas/ProblemDetail";
    private static final long   EXAMPLE_ID            = 99;
    private static final String EXAMPLE_INVALID_ID    = "abc";

    @Bean
    public OpenApiCustomizer problemDetailResponses() {
        return openApi -> {
            Components components = openApi.getComponents();
            if (components == null) {
                components = new Components();
                openApi.setComponents(components);
            }

            // no endpoint returns ProblemDetail directly, so its schema has to be registered by hand
            ModelConverters.getInstance().read(ProblemDetail.class).forEach(components::addSchemas);

            for (DocumentedResource resource : RESOURCES) {
                components.addResponses(resource.key() + NOT_FOUND, problemResponse(
                        "No %s exists with this id".formatted(resource.name().toLowerCase()),
                        // detail matches ResourceNotFoundException's message
                        example("Resource not found",
                                404,
                                "%s with id %d not found".formatted(resource.name(), EXAMPLE_ID),
                                resource.path() + "/" + EXAMPLE_ID
                        )
                ));
                components.addResponses(resource.key() + BAD_REQUEST, problemResponse(
                        "The id is not a number",
                        // detail matches Spring MVC's message for a path variable that fails type conversion
                        example("Bad Request",
                                400,
                                "Failed to convert 'id' with value: '%s'".formatted(EXAMPLE_INVALID_ID),
                                resource.path() + "/" + EXAMPLE_INVALID_ID
                        )
                ));
            }
        };
    }

    private static ApiResponse problemResponse(String description, Map<String, Object> example) {
        MediaType problemJson = new MediaType().schema(new Schema<>().$ref(PROBLEM_DETAIL_SCHEMA)).example(example);
        return new ApiResponse().description(description)
                .content(new Content().addMediaType(org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                        problemJson
                ));
    }

    // same fields, in the same order, as the problem details the API actually returns
    private static Map<String, Object> example(String title, int status, String detail, String instance) {
        Map<String, Object> example = new LinkedHashMap<>();
        example.put("title", title);
        example.put("status", status);
        example.put("detail", detail);
        example.put("instance", instance);
        return example;
    }

    /**
     * @param key  prefix of the resource's component names, e.g. {@code PizzaType}
     * @param name resource name as used in error messages, from {@link ApiResources}
     * @param path collection path of the resource, from {@link ApiResources}
     */
    private record DocumentedResource(String key, String name, String path) {
    }
}
