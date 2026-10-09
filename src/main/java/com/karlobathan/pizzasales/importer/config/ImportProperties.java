package com.karlobathan.pizzasales.importer.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.import")
public record ImportProperties(
        String pizzaTypesFile,
        String pizzasFile
) {
}
