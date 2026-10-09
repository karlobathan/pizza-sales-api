package com.karlobathan.pizzasales.importer.config;

/**
 * Base configuration prefixes for the application's own properties (everything under {@code app}).
 * The {@code @ConfigurationProperties} records build their prefixes from these constants.
 */
public final class AppProperties {

    public static final String PREFIX        = "app";
    public static final String IMPORT_PREFIX = PREFIX + ".import";

    private AppProperties() {
    }
}
