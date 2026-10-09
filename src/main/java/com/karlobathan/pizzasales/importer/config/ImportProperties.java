package com.karlobathan.pizzasales.importer.config;

import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "app.import")
@Validated
public record ImportProperties(
        String pizzaTypesFile,
        String pizzasFile,
        String ordersFile,
        @Positive int chunkSize // rows persisted per transaction for large files
) {
}
