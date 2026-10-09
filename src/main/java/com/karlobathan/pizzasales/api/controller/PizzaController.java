package com.karlobathan.pizzasales.api.controller;

import com.karlobathan.pizzasales.api.ApiResources;
import com.karlobathan.pizzasales.api.config.ApiDocs;
import com.karlobathan.pizzasales.api.dto.PizzaCreateRequest;
import com.karlobathan.pizzasales.api.dto.PizzaResponse;
import com.karlobathan.pizzasales.api.dto.PizzaUpdateRequest;
import com.karlobathan.pizzasales.api.service.PizzaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping(ApiResources.PIZZAS_PATH)
@RequiredArgsConstructor
@Tag(name = "Pizzas", description = "Sellable pizzas: each size and price of a pizza type")
public class PizzaController {

    private final PizzaService pizzaService;

    @GetMapping
    @Operation(summary = "List all pizzas", description = "Returns every pizza (size and price variant, deleted ones left out), ordered by id.")
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

    @PostMapping
    @Operation(summary = "Create a pizza",
            description = "Creates a pizza: one size and price of a pizza type. The Location header points to the new pizza."
    )
    @ApiResponse(responseCode = "201", description = "Pizza created")
    @ApiResponse(responseCode = "400", ref = ApiDocs.PIZZA_WRITE_BAD_REQUEST)
    @ApiResponse(responseCode = "409", ref = ApiDocs.PIZZA_CONFLICT)
    public ResponseEntity<PizzaResponse> create(@Valid @RequestBody PizzaCreateRequest request) {
        PizzaResponse created = pizzaService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(created.id()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Replace a pizza",
            description = "Replaces the pizza's pizza type, size and price; the code can't be changed. Orders show "
                    + "their totals at the current price, so a price change also changes past orders' totals. "
                    + "A deleted pizza can't be replaced (404)."
    )
    @ApiResponse(responseCode = "200", description = "Pizza replaced")
    @ApiResponse(responseCode = "404", ref = ApiDocs.PIZZA_NOT_FOUND)
    @ApiResponse(responseCode = "400", ref = ApiDocs.PIZZA_WRITE_BAD_REQUEST)
    @ApiResponse(responseCode = "409", ref = ApiDocs.PIZZA_CONFLICT)
    public PizzaResponse replace(@PathVariable Long id, @Valid @RequestBody PizzaUpdateRequest request) {
        return pizzaService.replace(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a pizza",
            description = "Deletes the pizza: it no longer appears on the menu and new orders can't use it, but orders "
                    + "already placed with it still show it. It is kept in the database (soft delete), so a re-import "
                    + "doesn't recreate it."
    )
    @ApiResponse(responseCode = "204", description = "Pizza deleted")
    @ApiResponse(responseCode = "404", ref = ApiDocs.PIZZA_NOT_FOUND)
    @ApiResponse(responseCode = "400", ref = ApiDocs.PIZZA_BAD_REQUEST)
    public void delete(@PathVariable Long id) {
        pizzaService.delete(id);
    }
}
