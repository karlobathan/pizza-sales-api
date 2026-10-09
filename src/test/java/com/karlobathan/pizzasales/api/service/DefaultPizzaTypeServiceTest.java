package com.karlobathan.pizzasales.api.service;

import com.karlobathan.pizzasales.api.domain.PizzaCategory;
import com.karlobathan.pizzasales.api.domain.PizzaIngredient;
import com.karlobathan.pizzasales.api.domain.PizzaType;
import com.karlobathan.pizzasales.api.dto.PizzaTypeCreateRequest;
import com.karlobathan.pizzasales.api.dto.PizzaTypeResponse;
import com.karlobathan.pizzasales.api.dto.PizzaTypeUpdateRequest;
import com.karlobathan.pizzasales.api.exception.ConflictException;
import com.karlobathan.pizzasales.api.exception.ResourceNotFoundException;
import com.karlobathan.pizzasales.api.mapper.PizzaMapper;
import com.karlobathan.pizzasales.api.repository.PizzaCategoryRepository;
import com.karlobathan.pizzasales.api.repository.PizzaIngredientRepository;
import com.karlobathan.pizzasales.api.repository.PizzaRepository;
import com.karlobathan.pizzasales.api.repository.PizzaTypeRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DefaultPizzaTypeServiceTest {

    private static final PizzaTypeResponse RESPONSE = new PizzaTypeResponse(1L, "pepperoni", "The Pepperoni Pizza", "Classic", List.of());

    @Mock
    private PizzaTypeRepository pizzaTypeRepository;

    @Mock
    private PizzaRepository pizzaRepository;

    @Mock
    private PizzaCategoryRepository pizzaCategoryRepository;

    @Mock
    private PizzaIngredientRepository pizzaIngredientRepository;

    @Mock
    private PizzaMapper pizzaMapper;

    @InjectMocks
    private DefaultPizzaTypeService service;

    @Test
    @DisplayName("findAll maps every active pizza type loaded with its details")
    void findAll_mapsEveryActivePizzaTypeLoadedWithDetails() {
        List<PizzaType> pizzaTypes = List.of(PizzaType.builder().id(1L).build());
        when(pizzaTypeRepository.findAllActiveWithDetails()).thenReturn(pizzaTypes);
        when(pizzaMapper.toPizzaTypeResponses(pizzaTypes)).thenReturn(List.of(RESPONSE));

        assertThat(service.findAll()).containsExactly(RESPONSE);
    }

    @Test
    @DisplayName("findById maps the active pizza type loaded with its details")
    void findById_mapsActivePizzaTypeLoadedWithDetails() {
        PizzaType pizzaType = PizzaType.builder().id(1L).build();
        when(pizzaTypeRepository.findActiveWithDetailsById(1L)).thenReturn(Optional.of(pizzaType));
        when(pizzaMapper.toResponse(pizzaType)).thenReturn(RESPONSE);

        assertThat(service.findById(1L)).isSameAs(RESPONSE);
    }

    @Test
    @DisplayName("findById throws resource not found naming the pizza type and id")
    void findById_throwsResourceNotFound() {
        when(pizzaTypeRepository.findActiveWithDetailsById(99L)).thenReturn(Optional.empty());

        assertThatExceptionOfType(ResourceNotFoundException.class).isThrownBy(() -> service.findById(99L))
                .withMessage("Pizza type with id 99 not found");
        verifyNoInteractions(pizzaMapper);
    }

    @Test
    @DisplayName("create reuses existing category and ingredients ignoring case and creates the missing ones")
    void create_reusesExistingAndCreatesMissing() {
        PizzaCategory classic = PizzaCategory.builder().id(3L).name("Classic").build();
        PizzaIngredient pepperoni = PizzaIngredient.builder().id(5L).name("Pepperoni").build();
        when(pizzaCategoryRepository.findByNameIgnoreCase("classic")).thenReturn(Optional.of(classic));
        when(pizzaIngredientRepository.findByLowerCaseNameIn(anyCollection())).thenReturn(List.of(pepperoni));
        when(pizzaIngredientRepository.save(any(PizzaIngredient.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(pizzaTypeRepository.save(any(PizzaType.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(pizzaMapper.toResponse(any(PizzaType.class))).thenReturn(RESPONSE);

        PizzaTypeResponse result = service.create(new PizzaTypeCreateRequest("pepperoni",
                " The Pepperoni Pizza ",
                "classic",
                List.of("PEPPERONI", " Basil ", "basil")
        ));

        assertThat(result).isSameAs(RESPONSE);
        ArgumentCaptor<PizzaType> saved = ArgumentCaptor.forClass(PizzaType.class);
        verify(pizzaTypeRepository).save(saved.capture());
        assertThat(saved.getValue().getCode()).isEqualTo("pepperoni");
        assertThat(saved.getValue().getName()).isEqualTo("The Pepperoni Pizza");
        assertThat(saved.getValue().getPizzaCategory()).isSameAs(classic);
        assertThat(saved.getValue().getPizzaIngredients()).extracting(PizzaIngredient::getName)
                .containsExactlyInAnyOrder("Pepperoni", "Basil");
        verify(pizzaIngredientRepository).findByLowerCaseNameIn(Set.of("pepperoni", "basil"));
        verify(pizzaCategoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("create creates a category that doesn't exist yet with the given spelling")
    void create_createsMissingCategory() {
        when(pizzaCategoryRepository.findByNameIgnoreCase("Dessert")).thenReturn(Optional.empty());
        when(pizzaCategoryRepository.save(any(PizzaCategory.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(pizzaIngredientRepository.findByLowerCaseNameIn(anyCollection())).thenReturn(List.of());
        when(pizzaIngredientRepository.save(any(PizzaIngredient.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(pizzaTypeRepository.save(any(PizzaType.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.create(new PizzaTypeCreateRequest("nutella", "The Nutella Pizza", " Dessert ", List.of("Nutella")));

        ArgumentCaptor<PizzaCategory> category = ArgumentCaptor.forClass(PizzaCategory.class);
        verify(pizzaCategoryRepository).save(category.capture());
        assertThat(category.getValue().getName()).isEqualTo("Dessert");
    }

    @Test
    @DisplayName("create rejects a code that is already used without saving anything")
    void create_rejectsTakenCode() {
        when(pizzaTypeRepository.existsByCode("pepperoni")).thenReturn(true);

        assertThatExceptionOfType(ConflictException.class).isThrownBy(() -> service.create(new PizzaTypeCreateRequest("pepperoni",
                "The Pepperoni Pizza",
                "Classic",
                List.of("Pepperoni")
        ))).withMessage("Pizza type code 'pepperoni' is already in use");
        verify(pizzaTypeRepository, never()).save(any());
        verifyNoInteractions(pizzaCategoryRepository, pizzaIngredientRepository);
    }

    @Test
    @DisplayName("replace changes name, category and ingredients but keeps the code")
    void replace_changesDetailsButKeepsCode() {
        PizzaType pizzaType = PizzaType.builder()
                .id(1L)
                .code("pepperoni")
                .name("Old")
                .pizzaCategory(PizzaCategory.builder().id(1L).name("Old").build())
                .pizzaIngredients(new HashSet<>(Set.of(PizzaIngredient.builder().id(9L).name("Old").build())))
                .build();
        PizzaCategory veggie = PizzaCategory.builder().id(4L).name("Veggie").build();
        when(pizzaTypeRepository.findActiveWithDetailsById(1L)).thenReturn(Optional.of(pizzaType));
        when(pizzaCategoryRepository.findByNameIgnoreCase("Veggie")).thenReturn(Optional.of(veggie));
        when(pizzaIngredientRepository.findByLowerCaseNameIn(anyCollection())).thenReturn(List.of());
        when(pizzaIngredientRepository.save(any(PizzaIngredient.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(pizzaMapper.toResponse(pizzaType)).thenReturn(RESPONSE);

        assertThat(service.replace(1L, new PizzaTypeUpdateRequest("New", "Veggie", List.of("Basil")))).isSameAs(RESPONSE);

        assertThat(pizzaType.getCode()).isEqualTo("pepperoni");
        assertThat(pizzaType.getName()).isEqualTo("New");
        assertThat(pizzaType.getPizzaCategory()).isSameAs(veggie);
        assertThat(pizzaType.getPizzaIngredients()).extracting(PizzaIngredient::getName).containsExactly("Basil");
    }

    @Test
    @DisplayName("replace throws resource not found without touching categories or ingredients")
    void replace_throwsResourceNotFound() {
        when(pizzaTypeRepository.findActiveWithDetailsById(99L)).thenReturn(Optional.empty());

        assertThatExceptionOfType(ResourceNotFoundException.class).isThrownBy(() -> service.replace(99L,
                new PizzaTypeUpdateRequest("New", "Veggie", List.of("Basil"))
        )).withMessage("Pizza type with id 99 not found");
        verifyNoInteractions(pizzaCategoryRepository, pizzaIngredientRepository);
    }

    @Test
    @DisplayName("delete marks a pizza type without pizzas deleted")
    void delete_marksPizzaTypeDeleted() {
        PizzaType pizzaType = PizzaType.builder().id(1L).build();
        when(pizzaTypeRepository.findActiveWithDetailsById(1L)).thenReturn(Optional.of(pizzaType));
        Instant before = Instant.now();

        service.delete(1L);

        assertThat(pizzaType.getDeletedAt()).isBetween(before, Instant.now());
        verify(pizzaTypeRepository, never()).delete(any());
    }

    @Test
    @DisplayName("delete rejects a pizza type that still has pizzas and leaves it active")
    void delete_rejectsPizzaTypeWithPizzas() {
        PizzaType pizzaType = PizzaType.builder().id(1L).build();
        when(pizzaTypeRepository.findActiveWithDetailsById(1L)).thenReturn(Optional.of(pizzaType));
        when(pizzaRepository.existsByPizzaTypeIdAndDeletedAtIsNull(1L)).thenReturn(true);

        assertThatExceptionOfType(ConflictException.class).isThrownBy(() -> service.delete(1L))
                .withMessage("Pizza type with id 1 still has pizzas; delete them first");
        assertThat(pizzaType.getDeletedAt()).isNull();
    }

    @Test
    @DisplayName("delete throws resource not found for an unknown or deleted pizza type")
    void delete_throwsResourceNotFound() {
        when(pizzaTypeRepository.findActiveWithDetailsById(99L)).thenReturn(Optional.empty());

        assertThatExceptionOfType(ResourceNotFoundException.class).isThrownBy(() -> service.delete(99L));
        verifyNoInteractions(pizzaRepository);
    }
}
