package com.karlobathan.pizzasales.importer.service;

import com.karlobathan.pizzasales.api.domain.Pizza;
import com.karlobathan.pizzasales.api.domain.PizzaType;
import com.karlobathan.pizzasales.api.repository.PizzaRepository;
import com.karlobathan.pizzasales.api.repository.PizzaTypeRepository;
import com.karlobathan.pizzasales.importer.loader.CsvResourceLoader;
import com.karlobathan.pizzasales.importer.loader.CsvSource;
import com.karlobathan.pizzasales.importer.row.PizzaCsvRow;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Service responsible for importing pizzas (size/price variants of a pizza type) from a CSV resource.
 * It checks if a pizza already exists in the repository based on its code.
 * If it does not exist, it creates a new pizza linked to its already imported pizza type.
 * Fails the import if a row references a pizza type that does not exist.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PizzaImportService implements Consumer<String> {

    private final PizzaRepository     pizzaRepository;
    private final PizzaTypeRepository pizzaTypeRepository;
    private final CsvResourceLoader   csvResourceLoader;

    @Override
    public void accept(String resourcePath) {
        CsvSource<PizzaCsvRow> pizzaSource = csvResourceLoader.loadResource(resourcePath, PizzaCsvRow.class);

        Set<String> existingCodes = pizzaRepository.findAllCode();
        Map<String, PizzaType> existingPizzaTypes = getExistingPizzaTypes();

        AtomicInteger existingCount = new AtomicInteger();
        List<Pizza> pizzasToAdd = new ArrayList<>();
        pizzaSource.forEach(row -> {
            String pizzaId = row.getPizzaId();
            if (existingCodes.contains(pizzaId)) {
                existingCount.incrementAndGet();
                log.debug("Pizza with ID {} already exists, skipping import.", pizzaId);
                return;
            }

            pizzasToAdd.add(getPizza(row, existingPizzaTypes));
        });

        pizzaRepository.saveAll(pizzasToAdd);
        log.info("Imported {} new pizzas ({} already existed)", pizzasToAdd.size(), existingCount.get());
    }

    private Pizza getPizza(PizzaCsvRow row, Map<String, PizzaType> existingPizzaTypes) {
        PizzaType pizzaType = existingPizzaTypes.get(row.getPizzaTypeId());
        if (pizzaType == null) {
            throw new IllegalStateException("Pizza '%s' references unknown pizza type '%s'".formatted(row.getPizzaId(),
                    row.getPizzaTypeId()
            ));
        }

        return Pizza.builder()
                .code(row.getPizzaId())
                .pizzaType(pizzaType)
                .size(row.getSize())
                .price(row.getPrice())
                .build();
    }

    private Map<String, PizzaType> getExistingPizzaTypes() {
        return pizzaTypeRepository.findAll()
                .stream()
                .collect(Collectors.toMap(PizzaType::getCode, Function.identity()));
    }
}
