package com.karlobathan.pizzasales.importer.config;

import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Settings shared by every import job ({@code app.import}).
 */
@ConfigurationProperties(prefix = AppProperties.IMPORT_PREFIX)
@Validated
public record ImportProperties(
        @Positive int chunkSize // rows persisted per transaction for large files
) {
}
