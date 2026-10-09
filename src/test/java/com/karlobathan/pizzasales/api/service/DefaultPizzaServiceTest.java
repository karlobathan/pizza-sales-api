package com.karlobathan.pizzasales.api.service;

import com.karlobathan.pizzasales.api.domain.Pizza;
import com.karlobathan.pizzasales.api.domain.PizzaSize;
import com.karlobathan.pizzasales.api.dto.PizzaResponse;
import com.karlobathan.pizzasales.api.dto.PizzaTypeSummary;
import com.karlobathan.pizzasales.api.exception.ResourceNotFoundException;
import com.karlobathan.pizzasales.api.mapper.PizzaMapper;
import com.karlobathan.pizzasales.api.repository.PizzaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
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

    @Mock
    private PizzaRepository pizzaRepository;

    @Mock
    private PizzaMapper pizzaMapper;

    @InjectMocks
    private DefaultPizzaService service;

    @Test
    @DisplayName("findAll maps every pizza loaded in id order with its pizza type")
    void findAll_mapsEveryPizzaInIdOrder() {
        List<Pizza> pizzas = List.of(Pizza.builder().id(10L).build());
        when(pizzaRepository.findAllByOrderByIdAsc()).thenReturn(pizzas);
        when(pizzaMapper.toPizzaResponses(pizzas)).thenReturn(List.of(PEPPERONI_M));

        assertThat(service.findAll()).containsExactly(PEPPERONI_M);
    }

    @Test
    @DisplayName("findById maps the pizza loaded with its pizza type")
    void findById_mapsPizzaLoadedWithPizzaType() {
        Pizza pizza = Pizza.builder().id(10L).build();
        when(pizzaRepository.findWithPizzaTypeById(10L)).thenReturn(Optional.of(pizza));
        when(pizzaMapper.toResponse(pizza)).thenReturn(PEPPERONI_M);

        assertThat(service.findById(10L)).isSameAs(PEPPERONI_M);
    }

    @Test
    @DisplayName("findById throws resource not found naming the pizza and id")
    void findById_throwsResourceNotFound() {
        when(pizzaRepository.findWithPizzaTypeById(99L)).thenReturn(Optional.empty());

        assertThatExceptionOfType(ResourceNotFoundException.class).isThrownBy(() -> service.findById(99L))
                .withMessage("Pizza with id 99 not found");
        verifyNoInteractions(pizzaMapper);
    }
}
