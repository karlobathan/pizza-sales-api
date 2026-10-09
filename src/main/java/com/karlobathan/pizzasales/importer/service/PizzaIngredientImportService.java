package com.karlobathan.pizzasales.importer.service;

import com.karlobathan.pizzasales.api.domain.PizzaIngredient;
import com.karlobathan.pizzasales.api.repository.PizzaIngredientRepository;
import com.karlobathan.pizzasales.importer.row.PizzaTypeCsvRow;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service responsible for importing pizza ingredients from CSV rows.
 * It checks if an ingredient already exists in the provided map of existing ingredients.
 * If it does not exist, it creates a new ingredient and saves it to the repository.
 */
@RequiredArgsConstructor
@Component
@Slf4j
public class PizzaIngredientImportService
        implements PizzaTypeCsvRowProcessor<Map<String, PizzaIngredient>, Set<PizzaIngredient>> {

    private final PizzaIngredientRepository pizzaIngredientRepository;

    @Override
    public Set<PizzaIngredient> apply(PizzaTypeCsvRow row, Map<String, PizzaIngredient> existingIngredients) {
        return row.getIngredients()
                .stream()
                .map(ingredient -> getPizzaIngredient(existingIngredients, ingredient))
                .collect(Collectors.toSet());
    }

    private PizzaIngredient getPizzaIngredient(Map<String, PizzaIngredient> existingIngredients, String ingredient) {
        PizzaIngredient pizzaIngredient = existingIngredients.get(ingredient.toLowerCase());
        if (pizzaIngredient != null) {
            return pizzaIngredient;
        }

        log.info("Ingredient '{}' does not exist, creating new ingredient.", ingredient);
        pizzaIngredient = PizzaIngredient.builder().name(ingredient).build();

        PizzaIngredient savedPizzaIngredient = pizzaIngredientRepository.save(pizzaIngredient);
        existingIngredients.put(ingredient.toLowerCase(), savedPizzaIngredient);
        return savedPizzaIngredient;
    }
}
