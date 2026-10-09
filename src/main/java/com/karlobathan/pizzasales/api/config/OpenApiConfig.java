package com.karlobathan.pizzasales.api.config;

import io.swagger.v3.core.converter.ModelConverters;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.ProblemDetail;

/**
 * OpenAPI setup shared by every resource. Each resource's error responses are registered by its own customizer
 * ({@link PizzaOpenApiCustomizer}, {@link OrderOpenApiCustomizer}); endpoints point to them through {@link ApiDocs}.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenApiCustomizer problemDetailSchema() {
        // no endpoint returns ProblemDetail directly, so the schema the error responses point to is registered by hand
        return openApi -> ModelConverters.getInstance()
                .read(ProblemDetail.class)
                .forEach(ProblemDetailResponses.components(openApi)::addSchemas);
    }
}
