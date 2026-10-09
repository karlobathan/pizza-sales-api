package com.karlobathan.pizzasales.importer.loader;

/**
 * Interface for loading CSV resources.
 */
public interface CsvResourceLoader {

    /**
     * Loads a CSV resource from the specified location and maps it to the given type.
     *
     * @param location the location of the CSV resource
     * @param type the class type to map the CSV rows to
     * @param <T> the type of the CSV row
     * @return a CsvSource representing the loaded CSV data
     */
    <T> CsvSource<T> loadResource(String location, Class<T> type);
}
