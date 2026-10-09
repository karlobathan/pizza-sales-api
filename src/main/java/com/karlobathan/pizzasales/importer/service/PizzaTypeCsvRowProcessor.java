package com.karlobathan.pizzasales.importer.service;

import com.karlobathan.pizzasales.importer.row.PizzaTypeCsvRow;

import java.util.function.BiFunction;

/**
 * A functional interface that processes a {@link PizzaTypeCsvRow} and returns a result of type R.
 *
 * @param <T> the type of the input to the function
 * @param <R> the type of the result of the function
 */
public interface PizzaTypeCsvRowProcessor<T, R> extends BiFunction<PizzaTypeCsvRow, T, R> {
}
