package com.karlobathan.pizzasales.importer.service;

import com.karlobathan.pizzasales.importer.config.ImportProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.function.Consumer;

/**
 * Orchestrates the import of pizza data from CSV files.
 * It uses the provided ImportProperties to determine the file paths and
 * delegates the actual import logic to the PizzaTypeImportService, PizzaImportService, OrderImportService
 * and OrderItemImportService. Files are imported in dependency order: pizza types before the pizzas that
 * reference them, and pizzas and orders before the order items that reference both.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PizzaImportOrchestrator implements Runnable {

    private final ImportProperties importProperties;
    private final Consumer<String> pizzaTypeImportService;
    private final Consumer<String> pizzaImportService;
    private final Consumer<String> orderImportService;
    private final Consumer<String> orderItemImportService;

    @Override
    public void run() {
        log.info("Starting pizza data loading from CSV files, using properties: {}", importProperties);

        pizzaTypeImportService.accept(importProperties.pizzaTypesFile());
        pizzaImportService.accept(importProperties.pizzasFile());
        orderImportService.accept(importProperties.ordersFile());
        orderItemImportService.accept(importProperties.orderDetailsFile());

        log.info("Pizza data loading finished.");
    }
}
