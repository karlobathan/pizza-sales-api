package com.karlobathan.pizzasales.api.mapper;

import com.karlobathan.pizzasales.api.domain.Pizza;
import com.karlobathan.pizzasales.api.domain.PizzaIngredient;
import com.karlobathan.pizzasales.api.domain.PizzaType;
import com.karlobathan.pizzasales.api.dto.PizzaResponse;
import com.karlobathan.pizzasales.api.dto.PizzaTypeResponse;
import com.karlobathan.pizzasales.api.dto.PizzaTypeSummary;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.Collection;
import java.util.List;

/**
 * Maps pizza entities to API responses. Callers must have fetched the associations being mapped
 * (category and ingredients for pizza types, pizza type for pizzas), as they are lazy.
 */
@Mapper(componentModel = "spring")
public interface PizzaMapper {

    @Mapping(target = "category", source = "pizzaCategory.name")
    @Mapping(target = "ingredients", source = "pizzaIngredients")
    PizzaTypeResponse toResponse(PizzaType pizzaType);

    PizzaTypeSummary toSummary(PizzaType pizzaType);

    PizzaResponse toResponse(Pizza pizza);

    List<PizzaTypeResponse> toPizzaTypeResponses(Collection<PizzaType> pizzaTypes);

    List<PizzaResponse> toPizzaResponses(Collection<Pizza> pizzas);

    default List<String> toIngredientNames(Collection<PizzaIngredient> ingredients) {
        return ingredients.stream().map(PizzaIngredient::getName).sorted().toList();
    }
}
