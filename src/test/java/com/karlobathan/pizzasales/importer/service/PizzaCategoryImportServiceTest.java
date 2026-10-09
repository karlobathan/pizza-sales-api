package com.karlobathan.pizzasales.importer.service;

import com.karlobathan.pizzasales.api.domain.PizzaCategory;
import com.karlobathan.pizzasales.api.repository.PizzaCategoryRepository;
import com.karlobathan.pizzasales.importer.row.PizzaTypeCsvRow;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PizzaCategoryImportServiceTest {

    @Mock
    private PizzaCategoryRepository pizzaCategoryRepository;

    @InjectMocks
    private PizzaCategoryImportService service;

    @Test
    @DisplayName("apply returns existing category without saving")
    void apply_returnsExistingCategoryWithoutSaving() {
        PizzaCategory classic = PizzaCategory.builder().id(1L).name("Classic").build();
        Map<String, PizzaCategory> existingCategories = new HashMap<>(Map.of("classic", classic));

        PizzaCategory result = service.apply(row("Classic"), existingCategories);

        assertThat(result).isSameAs(classic);
        assertThat(existingCategories).containsOnlyKeys("classic");
        verifyNoInteractions(pizzaCategoryRepository);
    }

    @Test
    @DisplayName("apply matches existing category case-insensitively")
    void apply_matchesExistingCategoryCaseInsensitively() {
        PizzaCategory classic = PizzaCategory.builder().id(1L).name("Classic").build();
        Map<String, PizzaCategory> existingCategories = new HashMap<>(Map.of("classic", classic));

        PizzaCategory result = service.apply(row("CLASSIC"), existingCategories);

        assertThat(result).isSameAs(classic);
        verifyNoInteractions(pizzaCategoryRepository);
    }

    @Test
    @DisplayName("apply saves missing category with its original spelling")
    void apply_savesMissingCategoryWithOriginalSpelling() {
        PizzaCategory saved = PizzaCategory.builder().id(7L).name("Supreme").build();
        when(pizzaCategoryRepository.save(any(PizzaCategory.class))).thenReturn(saved);

        PizzaCategory result = service.apply(row("Supreme"), new HashMap<>());

        ArgumentCaptor<PizzaCategory> captor = ArgumentCaptor.forClass(PizzaCategory.class);
        verify(pizzaCategoryRepository).save(captor.capture());
        assertThat(captor.getValue().getId()).isNull();
        assertThat(captor.getValue().getName()).isEqualTo("Supreme");
        assertThat(result).isSameAs(saved);
    }

    @Test
    @DisplayName("apply returns the same saved instance it caches")
    void apply_returnsSameSavedInstanceItCaches() {
        PizzaCategory saved = PizzaCategory.builder().id(7L).name("Supreme").build();
        when(pizzaCategoryRepository.save(any(PizzaCategory.class))).thenReturn(saved);
        Map<String, PizzaCategory> existingCategories = new HashMap<>();

        PizzaCategory result = service.apply(row("Supreme"), existingCategories);

        assertThat(result).isSameAs(existingCategories.get("supreme"));
        assertThat(result.getId()).isEqualTo(7L);
    }

    @Test
    @DisplayName("apply caches saved category under its lowercase name")
    void apply_cachesSavedCategoryUnderLowercaseName() {
        PizzaCategory saved = PizzaCategory.builder().id(7L).name("Supreme").build();
        when(pizzaCategoryRepository.save(any(PizzaCategory.class))).thenReturn(saved);
        Map<String, PizzaCategory> existingCategories = new HashMap<>();

        service.apply(row("Supreme"), existingCategories);

        assertThat(existingCategories).containsExactly(Map.entry("supreme", saved));
    }

    private static PizzaTypeCsvRow row(String category) {
        PizzaTypeCsvRow row = new PizzaTypeCsvRow();
        row.setCategory(category);
        return row;
    }
}
