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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PizzaTypeImportServiceTest {

    private static final String RESOURCE_PATH = "classpath:data/pizza_types.csv";

    @Mock
    private PizzaTypeRepository pizzaTypeRepository;

    @Mock
    private PizzaCategoryRepository pizzaCategoryRepository;

    @Mock
    private PizzaIngredientRepository pizzaIngredientRepository;

    @Mock
    private CsvResourceLoader csvResourceLoader;

    @Mock
    private PizzaTypeCsvRowProcessor<Map<String, PizzaIngredient>, Set<PizzaIngredient>> pizzaIngredientImportService;

    @Mock
    private PizzaTypeCsvRowProcessor<Map<String, PizzaCategory>, PizzaCategory> pizzaCategoryImportService;

    @Captor
    private ArgumentCaptor<Iterable<PizzaType>> pizzaTypesCaptor;

    @Captor
    private ArgumentCaptor<Map<String, PizzaCategory>> categoriesCaptor;

    @Captor
    private ArgumentCaptor<Map<String, PizzaIngredient>> ingredientsCaptor;

    private PizzaTypeImportService service;

    @BeforeEach
    void setUp() {
        // built by hand: the two processors share a raw type, so @InjectMocks could wire them the wrong way round
        service = new PizzaTypeImportService(pizzaTypeRepository,
                pizzaCategoryRepository,
                pizzaIngredientRepository,
                csvResourceLoader,
                pizzaIngredientImportService,
                pizzaCategoryImportService
        );
    }

    @Test
    @DisplayName("accept builds pizza types from rows using resolved category and ingredients")
    void accept_buildsPizzaTypesFromRows() {
        PizzaTypeCsvRow row = row("pepperoni", "The Pepperoni Pizza", "Classic", "Mozzarella Cheese", "Pepperoni");
        givenRows(row);
        PizzaCategory classic = PizzaCategory.builder().id(1L).name("Classic").build();
        Set<PizzaIngredient> ingredients = Set.of(PizzaIngredient.builder().id(1L).name("Mozzarella Cheese").build(),
                PizzaIngredient.builder().id(2L).name("Pepperoni").build()
        );
        when(pizzaCategoryImportService.apply(eq(row), anyMap())).thenReturn(classic);
        when(pizzaIngredientImportService.apply(eq(row), anyMap())).thenReturn(ingredients);

        service.accept(RESOURCE_PATH);

        verify(pizzaTypeRepository).saveAll(pizzaTypesCaptor.capture());
        assertThat(pizzaTypesCaptor.getValue()).singleElement().satisfies(pizzaType -> {
            assertThat(pizzaType.getId()).isNull();
            assertThat(pizzaType.getCode()).isEqualTo("pepperoni");
            assertThat(pizzaType.getName()).isEqualTo("The Pepperoni Pizza");
            assertThat(pizzaType.getPizzaCategory()).isSameAs(classic);
            assertThat(pizzaType.getPizzaIngredients()).isEqualTo(ingredients);
        });
    }

    @Test
    @DisplayName("accept skips rows whose code already exists")
    void accept_skipsRowsWhoseCodeAlreadyExists() {
        PizzaTypeCsvRow existing = row("pepperoni", "The Pepperoni Pizza", "Classic", "Pepperoni");
        PizzaTypeCsvRow fresh = row("hawaiian", "The Hawaiian Pizza", "Classic", "Pineapple");
        givenRows(existing, fresh);
        when(pizzaTypeRepository.findAllCode()).thenReturn(Set.of("pepperoni"));
        when(pizzaCategoryImportService.apply(eq(fresh), anyMap())).thenReturn(PizzaCategory.builder().build());
        when(pizzaIngredientImportService.apply(eq(fresh), anyMap())).thenReturn(Set.of());

        service.accept(RESOURCE_PATH);

        verify(pizzaCategoryImportService, never()).apply(eq(existing), anyMap());
        verify(pizzaIngredientImportService, never()).apply(eq(existing), anyMap());
        verify(pizzaTypeRepository).saveAll(pizzaTypesCaptor.capture());
        assertThat(pizzaTypesCaptor.getValue()).extracting(PizzaType::getCode).containsExactly("hawaiian");
    }

    @Test
    @DisplayName("accept passes existing categories and ingredients keyed by lowercase name to the processors")
    void accept_passesExistingLookupsKeyedByLowercaseName() {
        PizzaTypeCsvRow row = row("pepperoni", "The Pepperoni Pizza", "Classic", "Pepperoni");
        givenRows(row);
        PizzaCategory classic = PizzaCategory.builder().id(1L).name("Classic").build();
        PizzaIngredient pepperoni = PizzaIngredient.builder().id(2L).name("Pepperoni").build();
        when(pizzaCategoryRepository.findAll()).thenReturn(List.of(classic));
        when(pizzaIngredientRepository.findAll()).thenReturn(List.of(pepperoni));
        when(pizzaCategoryImportService.apply(eq(row), anyMap())).thenReturn(classic);
        when(pizzaIngredientImportService.apply(eq(row), anyMap())).thenReturn(Set.of(pepperoni));

        service.accept(RESOURCE_PATH);

        verify(pizzaCategoryImportService).apply(eq(row), categoriesCaptor.capture());
        verify(pizzaIngredientImportService).apply(eq(row), ingredientsCaptor.capture());
        assertThat(categoriesCaptor.getValue()).containsExactly(Map.entry("classic", classic));
        assertThat(ingredientsCaptor.getValue()).containsExactly(Map.entry("pepperoni", pepperoni));
    }

    @Test
    @DisplayName("accept shares the same lookup maps across rows")
    void accept_sharesSameLookupMapsAcrossRows() {
        PizzaTypeCsvRow first = row("pepperoni", "The Pepperoni Pizza", "Classic", "Pepperoni");
        PizzaTypeCsvRow second = row("hawaiian", "The Hawaiian Pizza", "Classic", "Pineapple");
        givenRows(first, second);
        when(pizzaCategoryImportService.apply(any(PizzaTypeCsvRow.class), anyMap())).thenReturn(PizzaCategory.builder()
                .build());
        when(pizzaIngredientImportService.apply(any(PizzaTypeCsvRow.class), anyMap())).thenReturn(Set.of());

        service.accept(RESOURCE_PATH);

        verify(pizzaCategoryImportService).apply(eq(first), categoriesCaptor.capture());
        verify(pizzaCategoryImportService).apply(eq(second), categoriesCaptor.capture());
        verify(pizzaIngredientImportService).apply(eq(first), ingredientsCaptor.capture());
        verify(pizzaIngredientImportService).apply(eq(second), ingredientsCaptor.capture());
        assertThat(categoriesCaptor.getAllValues().get(1)).isSameAs(categoriesCaptor.getAllValues().get(0));
        assertThat(ingredientsCaptor.getAllValues().get(1)).isSameAs(ingredientsCaptor.getAllValues().get(0));
    }

    @Test
    @DisplayName("accept saves nothing new when every code already exists")
    void accept_savesNothingNewWhenEveryCodeAlreadyExists() {
        givenRows(row("pepperoni", "The Pepperoni Pizza", "Classic", "Pepperoni"));
        when(pizzaTypeRepository.findAllCode()).thenReturn(Set.of("pepperoni"));

        service.accept(RESOURCE_PATH);

        verify(pizzaTypeRepository).saveAll(pizzaTypesCaptor.capture());
        assertThat(pizzaTypesCaptor.getValue()).isEmpty();
    }

    private void givenRows(PizzaTypeCsvRow... rows) {
        CsvSource<PizzaTypeCsvRow> source = action -> List.of(rows).forEach(action);
        when(csvResourceLoader.loadResource(RESOURCE_PATH, PizzaTypeCsvRow.class)).thenReturn(source);
    }

    private static PizzaTypeCsvRow row(String pizzaTypeId, String name, String category, String... ingredients) {
        PizzaTypeCsvRow row = new PizzaTypeCsvRow();
        row.setPizzaTypeId(pizzaTypeId);
        row.setName(name);
        row.setCategory(category);
        row.setIngredients(List.of(ingredients));
        return row;
    }
}
