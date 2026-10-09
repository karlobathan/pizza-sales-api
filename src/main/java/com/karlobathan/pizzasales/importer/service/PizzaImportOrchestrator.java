package com.karlobathan.pizzasales.importer.service;

import com.karlobathan.pizzasales.importer.config.PizzaImportProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.function.Consumer;

/**
 * Orchestrates the import of pizza data from CSV files.
 * It uses the provided PizzaImportProperties to determine the file paths and
 * delegates the actual import logic to the PizzaTypeImportService and PizzaImportService.
 * Pizza types are imported first because pizzas reference them.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PizzaImportOrchestrator implements Runnable {

    private final PizzaImportProperties pizzaImportProperties;
    private final Consumer<String>      pizzaTypeImportService;
    private final Consumer<String>      pizzaImportService;

    @Override
    public void run() {
        log.info("Starting pizza data loading from CSV files, using properties: {}", pizzaImportProperties);

        pizzaTypeImportService.accept(pizzaImportProperties.typesFile());
        pizzaImportService.accept(pizzaImportProperties.file());

        log.info("Pizza data loading finished.");
    }
}
