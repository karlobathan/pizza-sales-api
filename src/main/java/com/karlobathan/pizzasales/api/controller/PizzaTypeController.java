package com.karlobathan.pizzasales.api.controller;

import com.karlobathan.pizzasales.api.ApiResources;
import com.karlobathan.pizzasales.api.config.ApiDocs;
import com.karlobathan.pizzasales.api.dto.PizzaTypeResponse;
import com.karlobathan.pizzasales.api.service.PizzaTypeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(ApiResources.PIZZA_TYPES_PATH)
@RequiredArgsConstructor
@Tag(name = "Pizza types", description = "Browse the menu: pizza recipes with their category and ingredients")
public class PizzaTypeController {

    private final PizzaTypeService pizzaTypeService;

    @GetMapping
    @Operation(summary = "List all pizza types", description = "Returns every pizza type on the menu, ordered by id.")
    public List<PizzaTypeResponse> findAll() {
        return pizzaTypeService.findAll();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a pizza type by id")
    @ApiResponse(responseCode = "200", description = "Pizza type found")
    @ApiResponse(responseCode = "404", ref = ApiDocs.PIZZA_TYPE_NOT_FOUND)
    @ApiResponse(responseCode = "400", ref = ApiDocs.PIZZA_TYPE_BAD_REQUEST)
    public PizzaTypeResponse findById(@PathVariable Long id) {
        return pizzaTypeService.findById(id);
    }
}
