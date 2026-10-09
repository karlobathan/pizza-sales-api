package com.karlobathan.pizzasales.api.service;

import com.karlobathan.pizzasales.api.domain.PizzaType;
import com.karlobathan.pizzasales.api.dto.PizzaTypeResponse;
import com.karlobathan.pizzasales.api.exception.ResourceNotFoundException;
import com.karlobathan.pizzasales.api.mapper.PizzaMapper;
import com.karlobathan.pizzasales.api.repository.PizzaTypeRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DefaultPizzaTypeServiceTest {

    @Mock
    private PizzaTypeRepository pizzaTypeRepository;

    @Mock
    private PizzaMapper pizzaMapper;

    @InjectMocks
    private DefaultPizzaTypeService service;

    @Test
    @DisplayName("findAll maps every pizza type loaded with its details")
    void findAll_mapsEveryPizzaTypeLoadedWithDetails() {
        List<PizzaType> pizzaTypes = List.of(PizzaType.builder().id(1L).build());
        List<PizzaTypeResponse> responses = List.of(new PizzaTypeResponse(1L, "pepperoni", "The Pepperoni Pizza", "Classic", List.of()));
        when(pizzaTypeRepository.findAllWithDetails()).thenReturn(pizzaTypes);
        when(pizzaMapper.toPizzaTypeResponses(pizzaTypes)).thenReturn(responses);

        assertThat(service.findAll()).isSameAs(responses);
    }

    @Test
    @DisplayName("findById maps the pizza type loaded with its details")
    void findById_mapsPizzaTypeLoadedWithDetails() {
        PizzaType pizzaType = PizzaType.builder().id(1L).build();
        PizzaTypeResponse response = new PizzaTypeResponse(1L, "pepperoni", "The Pepperoni Pizza", "Classic", List.of());
        when(pizzaTypeRepository.findWithDetailsById(1L)).thenReturn(Optional.of(pizzaType));
        when(pizzaMapper.toResponse(pizzaType)).thenReturn(response);

        assertThat(service.findById(1L)).isSameAs(response);
    }

    @Test
    @DisplayName("findById throws resource not found naming the pizza type and id")
    void findById_throwsResourceNotFound() {
        when(pizzaTypeRepository.findWithDetailsById(99L)).thenReturn(Optional.empty());

        assertThatExceptionOfType(ResourceNotFoundException.class).isThrownBy(() -> service.findById(99L))
                .withMessage("Pizza type with id 99 not found");
        verifyNoInteractions(pizzaMapper);
    }
}
