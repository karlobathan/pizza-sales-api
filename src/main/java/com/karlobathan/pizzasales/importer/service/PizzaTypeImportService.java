package com.karlobathan.pizzasales.importer.service;

import com.karlobathan.pizzasales.api.domain.PizzaCategory;
import com.karlobathan.pizzasales.api.domain.PizzaIngredient;
import com.karlobathan.pizzasales.api.domain.PizzaType;
import com.karlobathan.pizzasales.api.repository.PizzaCategoryRepository;
import com.karlobathan.pizzasales.api.repository.PizzaIngredientRepository;
import com.karlobathan.pizzasales.api.repository.PizzaTypeRepository;
import com.karlobathan.pizzasales.importer.loader.CsvResourceLoader;
import com.karlobathan.pizzasales.importer.loader.CsvSource;
import com.karlobathan.pizzasales.importer.row.PizzaTypeCsvRow;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Service responsible for importing pizza types from a CSV resource.
 * It checks if a pizza type already exists in the repository based on its code.
 * If it does not exist, it creates a new pizza type and saves it to the repository.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PizzaTypeImportService implements Consumer<String> {

    private final PizzaTypeRepository                                                          pizzaTypeRepository;
    private final PizzaCategoryRepository                                                      pizzaCategoryRepository;
    private final PizzaIngredientRepository                                                    pizzaIngredientRepository;
    private final CsvResourceLoader                                                            csvResourceLoader;
    private final PizzaTypeCsvRowProcessor<Map<String, PizzaIngredient>, Set<PizzaIngredient>> pizzaIngredientImportService;
    private final PizzaTypeCsvRowProcessor<Map<String, PizzaCategory>, PizzaCategory>          pizzaCategoryImportService;

    @Override
    public void accept(String resourcePath) {
        CsvSource<PizzaTypeCsvRow> pizzaTypeSource = csvResourceLoader.loadResource(resourcePath,
                PizzaTypeCsvRow.class
        );

        Set<String> existingCodes = pizzaTypeRepository.findAllCode();
        Map<String, PizzaCategory> existingCategories = getExistingPizzaCategories();
        Map<String, PizzaIngredient> existingIngredients = getExistingPizzaIngredients();

        AtomicInteger existingCount = new AtomicInteger();
        Set<PizzaType> pizzaTypesToAdd = new HashSet<>();
        pizzaTypeSource.forEach(row -> {
            String pizzaTypeId = row.getPizzaTypeId();
            if (existingCodes.contains(pizzaTypeId)) {
                existingCount.incrementAndGet();
                log.debug("Pizza type with ID {} already exists, skipping import.", pizzaTypeId);
                return;
            }

            PizzaType pizzaType = getPizzaType(row, existingCategories, existingIngredients);
            pizzaTypesToAdd.add(pizzaType);
        });

        pizzaTypeRepository.saveAll(pizzaTypesToAdd);
        log.info("Imported {} new pizza types ({} already existed)", pizzaTypesToAdd.size(), existingCount.get());
    }

    private PizzaType getPizzaType(
            PizzaTypeCsvRow row,
            Map<String, PizzaCategory> existingCategories,
            Map<String, PizzaIngredient> existingIngredients) {
        PizzaCategory pizzaCategory = pizzaCategoryImportService.apply(row, existingCategories);
        Set<PizzaIngredient> pizzaIngredients = pizzaIngredientImportService.apply(row, existingIngredients);

        return PizzaType.builder()
                .code(row.getPizzaTypeId())
                .name(row.getName())
                .pizzaCategory(pizzaCategory)
                .pizzaIngredients(pizzaIngredients)
                .build();
    }

    private Map<String, PizzaIngredient> getExistingPizzaIngredients() {
        return pizzaIngredientRepository.findAll()
                .stream()
                .collect(Collectors.toMap(ingredient -> ingredient.getName().toLowerCase(), Function.identity()));
    }

    private Map<String, PizzaCategory> getExistingPizzaCategories() {
        return pizzaCategoryRepository.findAll()
                .stream()
                .collect(Collectors.toMap(category -> category.getName().toLowerCase(), Function.identity()));
    }
}
