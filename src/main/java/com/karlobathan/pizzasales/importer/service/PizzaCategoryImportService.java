package com.karlobathan.pizzasales.importer.service;

import com.karlobathan.pizzasales.api.domain.PizzaCategory;
import com.karlobathan.pizzasales.api.repository.PizzaCategoryRepository;
import com.karlobathan.pizzasales.importer.row.PizzaTypeCsvRow;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Service responsible for importing pizza categories from CSV rows.
 * It checks if a category already exists in the provided map of existing categories.
 * If it does not exist, it creates a new category and saves it to the repository.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PizzaCategoryImportService implements PizzaTypeCsvRowProcessor<Map<String, PizzaCategory>, PizzaCategory> {

    private final PizzaCategoryRepository pizzaCategoryRepository;

    @Override
    public PizzaCategory apply(PizzaTypeCsvRow row, Map<String, PizzaCategory> existingCategories) {
        PizzaCategory pizzaCategory = existingCategories.get(row.getCategory().toLowerCase());
        if (pizzaCategory == null) {
            log.info("Category '{}' does not exist, creating new category.", row.getCategory());

            pizzaCategory = pizzaCategoryRepository.save(PizzaCategory.builder().name(row.getCategory()).build());
            existingCategories.put(row.getCategory().toLowerCase(), pizzaCategory);
        }

        return pizzaCategory;
    }
}
