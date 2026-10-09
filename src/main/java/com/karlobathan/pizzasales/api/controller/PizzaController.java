package com.karlobathan.pizzasales.api.controller;

import com.karlobathan.pizzasales.api.ApiResources;
import com.karlobathan.pizzasales.api.config.ApiDocs;
import com.karlobathan.pizzasales.api.dto.PizzaResponse;
import com.karlobathan.pizzasales.api.service.PizzaService;
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
@RequestMapping(ApiResources.PIZZAS_PATH)
@RequiredArgsConstructor
@Tag(name = "Pizzas", description = "Sellable pizzas: each size and price of a pizza type")
public class PizzaController {

    private final PizzaService pizzaService;

    @GetMapping
    @Operation(summary = "List all pizzas", description = "Returns every pizza (size and price variant), ordered by id.")
    public List<PizzaResponse> findAll() {
        return pizzaService.findAll();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a pizza by id")
    @ApiResponse(responseCode = "200", description = "Pizza found")
    @ApiResponse(responseCode = "404", ref = ApiDocs.PIZZA_NOT_FOUND)
    @ApiResponse(responseCode = "400", ref = ApiDocs.PIZZA_BAD_REQUEST)
    public PizzaResponse findById(@PathVariable Long id) {
        return pizzaService.findById(id);
    }
}
