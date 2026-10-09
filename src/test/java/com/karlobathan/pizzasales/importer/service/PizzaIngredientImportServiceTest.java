package com.karlobathan.pizzasales.importer.service;

import com.karlobathan.pizzasales.api.domain.PizzaIngredient;
import com.karlobathan.pizzasales.api.repository.PizzaIngredientRepository;
import com.karlobathan.pizzasales.importer.row.PizzaTypeCsvRow;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PizzaIngredientImportServiceTest {

    @Mock
    private PizzaIngredientRepository pizzaIngredientRepository;

    @InjectMocks
    private PizzaIngredientImportService service;

    @Test
    @DisplayName("apply returns existing ingredients matched case-insensitively without saving")
    void apply_returnsExistingIngredientsWithoutSaving() {
        PizzaIngredient garlic = PizzaIngredient.builder().id(1L).name("Garlic").build();
        PizzaIngredient tomatoes = PizzaIngredient.builder().id(2L).name("Tomatoes").build();
        Map<String, PizzaIngredient> existingIngredients = new HashMap<>(Map.of("garlic", garlic, "tomatoes", tomatoes));

        Set<PizzaIngredient> result = service.apply(row("GARLIC", "Tomatoes"), existingIngredients);

        assertThat(result).containsExactlyInAnyOrder(garlic, tomatoes);
        assertThat(existingIngredients).containsOnlyKeys("garlic", "tomatoes");
        verifyNoInteractions(pizzaIngredientRepository);
    }

    @Test
    @DisplayName("apply saves only missing ingredients and caches them under lowercase names")
    void apply_savesOnlyMissingIngredientsAndCachesThem() {
        PizzaIngredient garlic = PizzaIngredient.builder().id(1L).name("Garlic").build();
        PizzaIngredient savedBasil = PizzaIngredient.builder().id(9L).name("Basil").build();
        when(pizzaIngredientRepository.save(any(PizzaIngredient.class))).thenReturn(savedBasil);
        Map<String, PizzaIngredient> existingIngredients = new HashMap<>(Map.of("garlic", garlic));

        Set<PizzaIngredient> result = service.apply(row("Garlic", "Basil"), existingIngredients);

        assertThat(result).containsExactlyInAnyOrder(garlic, savedBasil);
        assertThat(existingIngredients).containsEntry("basil", savedBasil);
        verify(pizzaIngredientRepository).save(argThat(ingredient -> ingredient.getId() == null
                && ingredient.getName().equals("Basil")));
    }

    @Test
    @DisplayName("apply saves a repeated new ingredient only once")
    void apply_savesRepeatedNewIngredientOnlyOnce() {
        PizzaIngredient savedBasil = PizzaIngredient.builder().id(9L).name("Basil").build();
        when(pizzaIngredientRepository.save(any(PizzaIngredient.class))).thenReturn(savedBasil);

        Set<PizzaIngredient> result = service.apply(row("Basil", "basil"), new HashMap<>());

        assertThat(result).containsExactly(savedBasil);
        verify(pizzaIngredientRepository, times(1)).save(any(PizzaIngredient.class));
    }

    @Test
    @DisplayName("apply returns empty set when row has no ingredients")
    void apply_returnsEmptySetWhenRowHasNoIngredients() {
        Set<PizzaIngredient> result = service.apply(row(), new HashMap<>());

        assertThat(result).isEmpty();
        verifyNoInteractions(pizzaIngredientRepository);
    }

    private static PizzaTypeCsvRow row(String... ingredients) {
        PizzaTypeCsvRow row = new PizzaTypeCsvRow();
        row.setIngredients(List.of(ingredients));
        return row;
    }
}
