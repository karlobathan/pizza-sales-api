package com.karlobathan.pizzasales.importer.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * CSV file locations for the pizza import job ({@code app.import.pizzas}).
 */
@ConfigurationProperties(prefix = AppProperties.IMPORT_PREFIX + ".pizzas")
public record PizzaImportProperties(
        String typesFile,
        String file
) {
}
