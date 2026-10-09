package com.karlobathan.pizzasales.importer.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * CSV file locations for the order import job ({@code app.import.orders}).
 */
@ConfigurationProperties(prefix = AppProperties.IMPORT_PREFIX + ".orders")
public record OrderImportProperties(
        String file,
        String detailsFile
) {
}
