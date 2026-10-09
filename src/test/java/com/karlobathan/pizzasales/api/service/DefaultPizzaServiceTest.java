package com.karlobathan.pizzasales.api.service;

import com.karlobathan.pizzasales.api.domain.Pizza;
import com.karlobathan.pizzasales.api.domain.PizzaSize;
import com.karlobathan.pizzasales.api.domain.PizzaType;
import com.karlobathan.pizzasales.api.dto.PizzaCreateRequest;
import com.karlobathan.pizzasales.api.dto.PizzaResponse;
import com.karlobathan.pizzasales.api.dto.PizzaTypeSummary;
import com.karlobathan.pizzasales.api.dto.PizzaUpdateRequest;
import com.karlobathan.pizzasales.api.exception.ConflictException;
import com.karlobathan.pizzasales.api.exception.InvalidRequestException;
import com.karlobathan.pizzasales.api.exception.ResourceNotFoundException;
import com.karlobathan.pizzasales.api.mapper.PizzaMapper;
import com.karlobathan.pizzasales.api.repository.PizzaRepository;
import com.karlobathan.pizzasales.api.repository.PizzaTypeRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DefaultPizzaServiceTest {

    private static final PizzaResponse PEPPERONI_M = new PizzaResponse(10L,
            "pepperoni_m",
            PizzaSize.M,
            new BigDecimal("12.50"),
            new PizzaTypeSummary(1L, "pepperoni", "The Pepperoni Pizza")
    );

    private final PizzaType pepperoni = PizzaType.builder().id(1L).code("pepperoni").build();

    @Mock
    private PizzaRepository pizzaRepository;

    @Mock
    private PizzaTypeRepository pizzaTypeRepository;

    @Mock
    private PizzaMapper pizzaMapper;

    @InjectMocks
    private DefaultPizzaService service;

    @Test
    @DisplayName("findAll maps every active pizza loaded in id order with its pizza type")
    void findAll_mapsEveryActivePizzaInIdOrder() {
        List<Pizza> pizzas = List.of(Pizza.builder().id(10L).build());
        when(pizzaRepository.findAllActiveWithPizzaType()).thenReturn(pizzas);
        when(pizzaMapper.toPizzaResponses(pizzas)).thenReturn(List.of(PEPPERONI_M));

        assertThat(service.findAll()).containsExactly(PEPPERONI_M);
    }

    @Test
    @DisplayName("findById maps the active pizza loaded with its pizza type")
    void findById_mapsActivePizzaLoadedWithPizzaType() {
        Pizza pizza = Pizza.builder().id(10L).build();
        when(pizzaRepository.findActiveWithPizzaTypeById(10L)).thenReturn(Optional.of(pizza));
        when(pizzaMapper.toResponse(pizza)).thenReturn(PEPPERONI_M);

        assertThat(service.findById(10L)).isSameAs(PEPPERONI_M);
    }

    @Test
    @DisplayName("findById throws resource not found naming the pizza and id")
    void findById_throwsResourceNotFound() {
        when(pizzaRepository.findActiveWithPizzaTypeById(99L)).thenReturn(Optional.empty());

        assertThatExceptionOfType(ResourceNotFoundException.class).isThrownBy(() -> service.findById(99L))
                .withMessage("Pizza with id 99 not found");
        verifyNoInteractions(pizzaMapper);
    }

    @Test
    @DisplayName("create saves the pizza with its pizza type, size and price")
    void create_savesPizza() {
        when(pizzaTypeRepository.findActiveWithDetailsById(1L)).thenReturn(Optional.of(pepperoni));
        when(pizzaRepository.save(any(Pizza.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(pizzaMapper.toResponse(any(Pizza.class))).thenReturn(PEPPERONI_M);

        assertThat(service.create(new PizzaCreateRequest("pepperoni_m", 1L, PizzaSize.M, new BigDecimal("12.50"))))
                .isSameAs(PEPPERONI_M);

        ArgumentCaptor<Pizza> saved = ArgumentCaptor.forClass(Pizza.class);
        verify(pizzaRepository).save(saved.capture());
        assertThat(saved.getValue().getId()).isNull();
        assertThat(saved.getValue().getCode()).isEqualTo("pepperoni_m");
        assertThat(saved.getValue().getPizzaType()).isSameAs(pepperoni);
        assertThat(saved.getValue().getSize()).isEqualTo(PizzaSize.M);
        assertThat(saved.getValue().getPrice()).isEqualByComparingTo("12.50");
    }

    @Test
    @DisplayName("create rejects a code that is already used")
    void create_rejectsTakenCode() {
        when(pizzaRepository.existsByCode("pepperoni_m")).thenReturn(true);

        assertThatExceptionOfType(ConflictException.class).isThrownBy(() -> service.create(new PizzaCreateRequest("pepperoni_m",
                1L,
                PizzaSize.M,
                BigDecimal.TEN
        ))).withMessage("Pizza code 'pepperoni_m' is already in use");
        verify(pizzaRepository, never()).save(any());
    }

    @Test
    @DisplayName("create rejects a size the pizza type already has")
    void create_rejectsTakenSize() {
        when(pizzaTypeRepository.findActiveWithDetailsById(1L)).thenReturn(Optional.of(pepperoni));
        when(pizzaRepository.existsByPizzaTypeIdAndSizeAndDeletedAtIsNull(1L, PizzaSize.M)).thenReturn(true);

        assertThatExceptionOfType(ConflictException.class).isThrownBy(() -> service.create(new PizzaCreateRequest("pepperoni_m2",
                1L,
                PizzaSize.M,
                BigDecimal.TEN
        ))).withMessage("Pizza type 'pepperoni' already has a size M pizza");
        verify(pizzaRepository, never()).save(any());
    }

    @Test
    @DisplayName("create rejects an unknown or deleted pizza type")
    void create_rejectsUnknownPizzaType() {
        when(pizzaTypeRepository.findActiveWithDetailsById(99L)).thenReturn(Optional.empty());

        assertThatExceptionOfType(InvalidRequestException.class).isThrownBy(() -> service.create(new PizzaCreateRequest("ghost_m",
                99L,
                PizzaSize.M,
                BigDecimal.TEN
        ))).withMessage("Unknown pizza type id: 99");
        verify(pizzaRepository, never()).save(any());
    }

    @Test
    @DisplayName("replace changes pizza type, size and price but keeps the code")
    void replace_changesDetailsButKeepsCode() {
        Pizza pizza = Pizza.builder().id(10L).code("pepperoni_m").size(PizzaSize.M).price(BigDecimal.ONE).build();
        PizzaType hawaiian = PizzaType.builder().id(2L).code("hawaiian").build();
        when(pizzaRepository.findActiveWithPizzaTypeById(10L)).thenReturn(Optional.of(pizza));
        when(pizzaTypeRepository.findActiveWithDetailsById(2L)).thenReturn(Optional.of(hawaiian));
        when(pizzaMapper.toResponse(pizza)).thenReturn(PEPPERONI_M);

        service.replace(10L, new PizzaUpdateRequest(2L, PizzaSize.L, new BigDecimal("15.25")));

        assertThat(pizza.getCode()).isEqualTo("pepperoni_m");
        assertThat(pizza.getPizzaType()).isSameAs(hawaiian);
        assertThat(pizza.getSize()).isEqualTo(PizzaSize.L);
        assertThat(pizza.getPrice()).isEqualByComparingTo("15.25");
        verify(pizzaRepository).existsByPizzaTypeIdAndSizeAndDeletedAtIsNullAndIdNot(2L, PizzaSize.L, 10L);
    }

    @Test
    @DisplayName("replace rejects a size another pizza of the pizza type already has and changes nothing")
    void replace_rejectsTakenSize() {
        Pizza pizza = Pizza.builder().id(10L).code("pepperoni_m").pizzaType(pepperoni).size(PizzaSize.M).build();
        when(pizzaRepository.findActiveWithPizzaTypeById(10L)).thenReturn(Optional.of(pizza));
        when(pizzaTypeRepository.findActiveWithDetailsById(1L)).thenReturn(Optional.of(pepperoni));
        when(pizzaRepository.existsByPizzaTypeIdAndSizeAndDeletedAtIsNullAndIdNot(1L, PizzaSize.L, 10L)).thenReturn(true);

        assertThatExceptionOfType(ConflictException.class).isThrownBy(() -> service.replace(10L,
                new PizzaUpdateRequest(1L, PizzaSize.L, BigDecimal.TEN)
        )).withMessage("Pizza type 'pepperoni' already has a size L pizza");
        assertThat(pizza.getSize()).isEqualTo(PizzaSize.M);
    }

    @Test
    @DisplayName("replace throws resource not found without looking up the pizza type")
    void replace_throwsResourceNotFound() {
        when(pizzaRepository.findActiveWithPizzaTypeById(99L)).thenReturn(Optional.empty());

        assertThatExceptionOfType(ResourceNotFoundException.class).isThrownBy(() -> service.replace(99L,
                new PizzaUpdateRequest(1L, PizzaSize.L, BigDecimal.TEN)
        ));
        verifyNoInteractions(pizzaTypeRepository);
    }

    @Test
    @DisplayName("delete marks the pizza deleted without removing it")
    void delete_marksPizzaDeleted() {
        Pizza pizza = Pizza.builder().id(10L).build();
        when(pizzaRepository.findActiveWithPizzaTypeById(10L)).thenReturn(Optional.of(pizza));
        Instant before = Instant.now();

        service.delete(10L);

        assertThat(pizza.getDeletedAt()).isBetween(before, Instant.now());
        verify(pizzaRepository, never()).delete(any());
    }

    @Test
    @DisplayName("delete throws resource not found for an unknown or deleted pizza")
    void delete_throwsResourceNotFound() {
        when(pizzaRepository.findActiveWithPizzaTypeById(99L)).thenReturn(Optional.empty());

        assertThatExceptionOfType(ResourceNotFoundException.class).isThrownBy(() -> service.delete(99L))
                .withMessage("Pizza with id 99 not found");
    }
}
