package com.karlobathan.pizzasales.importer.service;

import com.karlobathan.pizzasales.importer.config.OrderImportProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.function.Consumer;

/**
 * Orchestrates the import of order data from CSV files.
 * It uses the provided OrderImportProperties to determine the file paths and
 * delegates the actual import logic to the OrderImportService and OrderItemImportService.
 * Orders are imported first because order items reference them. Order items also reference pizzas,
 * so the pizza import ({@link PizzaImportOrchestrator}) must have run before this one.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderImportOrchestrator implements Runnable {

    private final OrderImportProperties orderImportProperties;
    private final Consumer<String>      orderImportService;
    private final Consumer<String>      orderItemImportService;

    @Override
    public void run() {
        log.info("Starting order data loading from CSV files, using properties: {}", orderImportProperties);

        orderImportService.accept(orderImportProperties.file());
        orderItemImportService.accept(orderImportProperties.detailsFile());

        log.info("Order data loading finished.");
    }
}
