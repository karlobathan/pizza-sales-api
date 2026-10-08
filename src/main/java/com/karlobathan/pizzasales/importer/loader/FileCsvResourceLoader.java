package com.karlobathan.pizzasales.importer.loader;

import com.opencsv.bean.CsvToBeanBuilder;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * Loads CSV data from a Spring {@link Resource} location (classpath: or file:),
 * decoding as UTF-8.
 */
public final class FileCsvResourceLoader implements CsvResourceLoader {

    private final ResourceLoader resourceLoader;

    public FileCsvResourceLoader(ResourceLoader resourceLoader) {
        this.resourceLoader = Objects.requireNonNull(resourceLoader, "resourceLoader must not be null");
    }

    @Override
    public <T> CsvSource<T> loadResource(String location, Class<T> type) {
        Resource resource = resourceLoader.getResource(location);

        return action -> {
            try (Reader reader = new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8)) {
                new CsvToBeanBuilder<T>(reader).withType(type)
                        .withIgnoreLeadingWhiteSpace(true)
                        .build()
                        .forEach(action);
            } catch (IOException e) {
                throw new RuntimeException("Failed to read CSV file at " + location, e);
            }
        };
    }
}
