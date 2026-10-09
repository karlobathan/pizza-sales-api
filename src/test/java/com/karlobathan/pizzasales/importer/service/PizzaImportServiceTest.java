package com.karlobathan.pizzasales.importer.service;

import com.karlobathan.pizzasales.api.domain.Pizza;
import com.karlobathan.pizzasales.api.domain.PizzaSize;
import com.karlobathan.pizzasales.api.domain.PizzaType;
import com.karlobathan.pizzasales.api.repository.PizzaRepository;
import com.karlobathan.pizzasales.api.repository.PizzaTypeRepository;
import com.karlobathan.pizzasales.importer.loader.CsvResourceLoader;
import com.karlobathan.pizzasales.importer.loader.CsvSource;
import com.karlobathan.pizzasales.importer.row.PizzaCsvRow;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PizzaImportServiceTest {

    private static final String RESOURCE_PATH = "classpath:data/pizzas.csv";

    @Mock
    private PizzaRepository pizzaRepository;

    @Mock
    private PizzaTypeRepository pizzaTypeRepository;

    @Mock
    private CsvResourceLoader csvResourceLoader;

    @Captor
    private ArgumentCaptor<Iterable<Pizza>> pizzasCaptor;

    @InjectMocks
    private PizzaImportService service;

    @Test
    @DisplayName("accept builds pizzas from rows linked to their pizza type")
    void accept_buildsPizzasFromRowsLinkedToPizzaType() {
        PizzaType pepperoni = PizzaType.builder().id(1L).code("pepperoni").build();
        when(pizzaTypeRepository.findAll()).thenReturn(List.of(pepperoni));
        givenRows(row("pepperoni_s", "pepperoni", PizzaSize.S, "9.75"));

        service.accept(RESOURCE_PATH);

        verify(pizzaRepository).saveAll(pizzasCaptor.capture());
        assertThat(pizzasCaptor.getValue()).singleElement().satisfies(pizza -> {
            assertThat(pizza.getId()).isNull();
            assertThat(pizza.getCode()).isEqualTo("pepperoni_s");
            assertThat(pizza.getPizzaType()).isSameAs(pepperoni);
            assertThat(pizza.getSize()).isEqualTo(PizzaSize.S);
            assertThat(pizza.getPrice()).isEqualByComparingTo("9.75");
        });
    }

    @Test
    @DisplayName("accept links every size of a pizza type to the same pizza type")
    void accept_linksEverySizeToSamePizzaType() {
        PizzaType pepperoni = PizzaType.builder().id(1L).code("pepperoni").build();
        PizzaType hawaiian = PizzaType.builder().id(2L).code("hawaiian").build();
        when(pizzaTypeRepository.findAll()).thenReturn(List.of(pepperoni, hawaiian));
        givenRows(row("pepperoni_s", "pepperoni", PizzaSize.S, "9.75"),
                row("pepperoni_l", "pepperoni", PizzaSize.L, "15.25"),
                row("hawaiian_m", "hawaiian", PizzaSize.M, "13.25")
        );

        service.accept(RESOURCE_PATH);

        verify(pizzaRepository).saveAll(pizzasCaptor.capture());
        assertThat(pizzasCaptor.getValue()).extracting(Pizza::getCode, pizza -> pizza.getPizzaType().getCode())
                .containsExactly(tuple("pepperoni_s", "pepperoni"),
                        tuple("pepperoni_l", "pepperoni"),
                        tuple("hawaiian_m", "hawaiian")
                );
    }

    @Test
    @DisplayName("accept skips rows whose code already exists")
    void accept_skipsRowsWhoseCodeAlreadyExists() {
        PizzaType pepperoni = PizzaType.builder().id(1L).code("pepperoni").build();
        when(pizzaTypeRepository.findAll()).thenReturn(List.of(pepperoni));
        when(pizzaRepository.findAllCode()).thenReturn(Set.of("pepperoni_s"));
        givenRows(row("pepperoni_s", "pepperoni", PizzaSize.S, "9.75"),
                row("pepperoni_m", "pepperoni", PizzaSize.M, "12.50")
        );

        service.accept(RESOURCE_PATH);

        verify(pizzaRepository).saveAll(pizzasCaptor.capture());
        assertThat(pizzasCaptor.getValue()).extracting(Pizza::getCode).containsExactly("pepperoni_m");
    }

    @Test
    @DisplayName("accept saves nothing new when every code already exists")
    void accept_savesNothingNewWhenEveryCodeAlreadyExists() {
        when(pizzaRepository.findAllCode()).thenReturn(Set.of("pepperoni_s"));
        givenRows(row("pepperoni_s", "pepperoni", PizzaSize.S, "9.75"));

        service.accept(RESOURCE_PATH);

        verify(pizzaRepository).saveAll(pizzasCaptor.capture());
        assertThat(pizzasCaptor.getValue()).isEmpty();
    }

    @Test
    @DisplayName("accept fails without saving when a row references an unknown pizza type")
    void accept_failsWithoutSavingWhenPizzaTypeIsUnknown() {
        PizzaType pepperoni = PizzaType.builder().id(1L).code("pepperoni").build();
        when(pizzaTypeRepository.findAll()).thenReturn(List.of(pepperoni));
        givenRows(row("pepperoni_s", "pepperoni", PizzaSize.S, "9.75"),
                row("ghost_m", "ghost", PizzaSize.M, "10.00")
        );

        assertThatIllegalStateException().isThrownBy(() -> service.accept(RESOURCE_PATH))
                .withMessage("Pizza 'ghost_m' references unknown pizza type 'ghost'");
        verify(pizzaRepository, never()).saveAll(any());
    }

    private void givenRows(PizzaCsvRow... rows) {
        CsvSource<PizzaCsvRow> source = action -> List.of(rows).forEach(action);
        when(csvResourceLoader.loadResource(RESOURCE_PATH, PizzaCsvRow.class)).thenReturn(source);
    }

    private static PizzaCsvRow row(String pizzaId, String pizzaTypeId, PizzaSize size, String price) {
        PizzaCsvRow row = new PizzaCsvRow();
        row.setPizzaId(pizzaId);
        row.setPizzaTypeId(pizzaTypeId);
        row.setSize(size);
        row.setPrice(new BigDecimal(price));
        return row;
    }
}
