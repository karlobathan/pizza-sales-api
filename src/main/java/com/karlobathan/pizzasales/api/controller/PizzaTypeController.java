package com.karlobathan.pizzasales.api.controller;

import com.karlobathan.pizzasales.api.ApiResources;
import com.karlobathan.pizzasales.api.config.ApiDocs;
import com.karlobathan.pizzasales.api.dto.PizzaTypeCreateRequest;
import com.karlobathan.pizzasales.api.dto.PizzaTypeResponse;
import com.karlobathan.pizzasales.api.dto.PizzaTypeUpdateRequest;
import com.karlobathan.pizzasales.api.service.PizzaTypeService;
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
@RequestMapping(ApiResources.PIZZA_TYPES_PATH)
@RequiredArgsConstructor
@Tag(name = "Pizza types", description = "Browse the menu: pizza recipes with their category and ingredients")
public class PizzaTypeController {

    private final PizzaTypeService pizzaTypeService;

    @GetMapping
    @Operation(summary = "List all pizza types", description = "Returns every pizza type on the menu (deleted ones left out), ordered by id.")
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

    @PostMapping
    @Operation(summary = "Create a pizza type",
            description = "Creates a pizza type. Its category and ingredients are matched by name, ignoring case, "
                    + "and created if they don't exist yet. The Location header points to the new pizza type."
    )
    @ApiResponse(responseCode = "201", description = "Pizza type created")
    @ApiResponse(responseCode = "400", ref = ApiDocs.PIZZA_TYPE_WRITE_BAD_REQUEST)
    @ApiResponse(responseCode = "409", ref = ApiDocs.PIZZA_TYPE_CONFLICT)
    public ResponseEntity<PizzaTypeResponse> create(@Valid @RequestBody PizzaTypeCreateRequest request) {
        PizzaTypeResponse created = pizzaTypeService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(created.id()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Replace a pizza type",
            description = "Replaces the pizza type's name, category and ingredients; the code can't be changed. "
                    + "A deleted pizza type can't be replaced (404)."
    )
    @ApiResponse(responseCode = "200", description = "Pizza type replaced")
    @ApiResponse(responseCode = "404", ref = ApiDocs.PIZZA_TYPE_NOT_FOUND)
    @ApiResponse(responseCode = "400", ref = ApiDocs.PIZZA_TYPE_WRITE_BAD_REQUEST)
    public PizzaTypeResponse replace(@PathVariable Long id, @Valid @RequestBody PizzaTypeUpdateRequest request) {
        return pizzaTypeService.replace(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a pizza type",
            description = "Deletes the pizza type: it no longer appears on the menu and new pizzas can't use it. "
                    + "Its pizzas must be deleted first (409 otherwise). It is kept in the database (soft delete), "
                    + "so a re-import doesn't recreate it."
    )
    @ApiResponse(responseCode = "204", description = "Pizza type deleted")
    @ApiResponse(responseCode = "404", ref = ApiDocs.PIZZA_TYPE_NOT_FOUND)
    @ApiResponse(responseCode = "400", ref = ApiDocs.PIZZA_TYPE_BAD_REQUEST)
    @ApiResponse(responseCode = "409", ref = ApiDocs.PIZZA_TYPE_CONFLICT)
    public void delete(@PathVariable Long id) {
        pizzaTypeService.delete(id);
    }
}
