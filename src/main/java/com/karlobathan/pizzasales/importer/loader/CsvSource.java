package com.karlobathan.pizzasales.importer.loader;

import java.util.function.Consumer;

/**
 * Represents a source of CSV data that can be iterated over.
 *
 * @param <T> the type of the CSV row
 */
@FunctionalInterface
public interface CsvSource<T> {

    /**
     * Iterates over each row in the CSV source and performs the given action.
     *
     * @param action the action to be performed for each row
     */
    void forEach(Consumer<? super T> action);
}
