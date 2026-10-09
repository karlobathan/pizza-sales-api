package com.karlobathan.pizzasales.api.mapper;

import com.karlobathan.pizzasales.api.domain.Pizza;
import com.karlobathan.pizzasales.api.domain.PizzaCategory;
import com.karlobathan.pizzasales.api.domain.PizzaIngredient;
import com.karlobathan.pizzasales.api.domain.PizzaSize;
import com.karlobathan.pizzasales.api.domain.PizzaType;
import com.karlobathan.pizzasales.api.dto.PizzaResponse;
import com.karlobathan.pizzasales.api.dto.PizzaTypeResponse;
import com.karlobathan.pizzasales.api.dto.PizzaTypeSummary;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class PizzaMapperTest {

    private final PizzaMapper mapper = Mappers.getMapper(PizzaMapper.class);

    @Test
    @DisplayName("toResponse maps pizza type with category name and ingredient names sorted")
    void toResponse_mapsPizzaTypeWithCategoryAndSortedIngredients() {
        PizzaType pizzaType = pizzaType();

        PizzaTypeResponse response = mapper.toResponse(pizzaType);

        assertThat(response).isEqualTo(new PizzaTypeResponse(1L,
                "pepperoni",
                "The Pepperoni Pizza",
                "Classic",
                List.of("Mozzarella Cheese", "Pepperoni")
        ));
    }

    @Test
    @DisplayName("toResponse maps pizza type without ingredients to an empty ingredient list")
    void toResponse_mapsPizzaTypeWithoutIngredientsToEmptyList() {
        PizzaType pizzaType = pizzaType();
        pizzaType.setPizzaIngredients(Set.of());

        assertThat(mapper.toResponse(pizzaType).ingredients()).isEmpty();
    }

    @Test
    @DisplayName("toResponse maps pizza with size, price and a pizza type summary")
    void toResponse_mapsPizzaWithPizzaTypeSummary() {
        Pizza pizza = Pizza.builder()
                .id(10L)
                .code("pepperoni_m")
                .size(PizzaSize.M)
                .price(new BigDecimal("12.50"))
                .pizzaType(pizzaType())
                .build();

        PizzaResponse response = mapper.toResponse(pizza);

        assertThat(response).isEqualTo(new PizzaResponse(10L,
                "pepperoni_m",
                PizzaSize.M,
                new BigDecimal("12.50"),
                new PizzaTypeSummary(1L, "pepperoni", "The Pepperoni Pizza")
        ));
    }

    @Test
    @DisplayName("toPizzaResponses keeps the order of the given pizzas")
    void toPizzaResponses_keepsOrder() {
        PizzaType pizzaType = pizzaType();
        List<Pizza> pizzas = List.of(Pizza.builder().id(2L).code("b").pizzaType(pizzaType).build(),
                Pizza.builder().id(1L).code("a").pizzaType(pizzaType).build()
        );

        assertThat(mapper.toPizzaResponses(pizzas)).extracting(PizzaResponse::id).containsExactly(2L, 1L);
    }

    private static PizzaType pizzaType() {
        return PizzaType.builder()
                .id(1L)
                .code("pepperoni")
                .name("The Pepperoni Pizza")
                .pizzaCategory(PizzaCategory.builder().id(3L).name("Classic").build())
                .pizzaIngredients(Set.of(PizzaIngredient.builder().id(5L).name("Pepperoni").build(),
                        PizzaIngredient.builder().id(4L).name("Mozzarella Cheese").build()
                ))
                .build();
    }
}
