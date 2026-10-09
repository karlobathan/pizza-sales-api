package com.karlobathan.pizzasales.api.controller;

import com.karlobathan.pizzasales.api.ApiResources;
import com.karlobathan.pizzasales.api.dto.PizzaTypeCreateRequest;
import com.karlobathan.pizzasales.api.dto.PizzaTypeResponse;
import com.karlobathan.pizzasales.api.dto.PizzaTypeUpdateRequest;
import com.karlobathan.pizzasales.api.exception.ConflictException;
import com.karlobathan.pizzasales.api.exception.ResourceNotFoundException;
import com.karlobathan.pizzasales.api.service.PizzaTypeService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PizzaTypeController.class)
class PizzaTypeControllerTest {

    private static final PizzaTypeResponse PEPPERONI = new PizzaTypeResponse(1L,
            "pepperoni",
            "The Pepperoni Pizza",
            "Classic",
            List.of("Mozzarella Cheese", "Pepperoni")
    );

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PizzaTypeService pizzaTypeService;

    @Test
    @DisplayName("GET /api/pizza-types returns every pizza type")
    void findAll_returnsEveryPizzaType() throws Exception {
        PizzaTypeResponse hawaiian = new PizzaTypeResponse(2L, "hawaiian", "The Hawaiian Pizza", "Classic", List.of());
        when(pizzaTypeService.findAll()).thenReturn(List.of(PEPPERONI, hawaiian));

        mockMvc.perform(get("/api/pizza-types"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].code", contains("pepperoni", "hawaiian")));
    }

    @Test
    @DisplayName("GET /api/pizza-types returns an empty list when there are none")
    void findAll_returnsEmptyList() throws Exception {
        when(pizzaTypeService.findAll()).thenReturn(List.of());

        mockMvc.perform(get("/api/pizza-types")).andExpect(status().isOk()).andExpect(content().json("[]"));
    }

    @Test
    @DisplayName("GET /api/pizza-types/{id} returns the pizza type")
    void findById_returnsPizzaType() throws Exception {
        when(pizzaTypeService.findById(1L)).thenReturn(PEPPERONI);

        mockMvc.perform(get("/api/pizza-types/1")).andExpect(status().isOk()).andExpect(content().json("""
                {
                  "id": 1,
                  "code": "pepperoni",
                  "name": "The Pepperoni Pizza",
                  "category": "Classic",
                  "ingredients": ["Mozzarella Cheese", "Pepperoni"]
                }
                """, true));
    }

    @Test
    @DisplayName("GET /api/pizza-types/{id} returns 404 problem detail when the pizza type does not exist")
    void findById_returnsNotFoundProblemDetail() throws Exception {
        when(pizzaTypeService.findById(99L)).thenThrow(new ResourceNotFoundException(ApiResources.PIZZA_TYPE, 99L));

        mockMvc.perform(get("/api/pizza-types/99"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.title").value("Resource not found"))
                .andExpect(jsonPath("$.detail").value("Pizza type with id 99 not found"))
                .andExpect(jsonPath("$.instance").value("/api/pizza-types/99"));
    }

    @Test
    @DisplayName("GET /api/pizza-types/{id} returns 400 problem detail when the id is not a number")
    void findById_returnsBadRequestWhenIdIsNotANumber() throws Exception {
        mockMvc.perform(get("/api/pizza-types/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400));
        verifyNoInteractions(pizzaTypeService);
    }

    @Test
    @DisplayName("POST /api/pizza-types returns 201 with the created pizza type and its location")
    void create_returnsCreatedWithLocation() throws Exception {
        when(pizzaTypeService.create(new PizzaTypeCreateRequest("pepperoni",
                "The Pepperoni Pizza",
                "Classic",
                List.of("Mozzarella Cheese", "Pepperoni")
        ))).thenReturn(PEPPERONI);

        mockMvc.perform(post("/api/pizza-types").contentType(MediaType.APPLICATION_JSON).content("""
                        {"code": "pepperoni", "name": "The Pepperoni Pizza", "category": "Classic",
                         "ingredients": ["Mozzarella Cheese", "Pepperoni"]}
                        """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/pizza-types/1"))
                .andExpect(jsonPath("$.code").value("pepperoni"));
    }

    @Test
    @DisplayName("POST /api/pizza-types returns 400 listing every invalid field without calling the service")
    void create_returnsBadRequestListingInvalidFields() throws Exception {
        mockMvc.perform(post("/api/pizza-types").contentType(MediaType.APPLICATION_JSON).content("""
                        {"code": "Pepperoni Pizza", "name": " ", "ingredients": []}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(content().json("""
                        {"errors": [
                          {"field": "category", "message": "must not be blank"},
                          {"field": "code", "message": "must contain only lowercase letters, digits and underscores"},
                          {"field": "ingredients", "message": "must not be empty"},
                          {"field": "name", "message": "must not be blank"}
                        ]}
                        """));
        verifyNoInteractions(pizzaTypeService);
    }

    @Test
    @DisplayName("POST /api/pizza-types returns 409 problem detail when the code is taken")
    void create_returnsConflictWhenCodeIsTaken() throws Exception {
        when(pizzaTypeService.create(any())).thenThrow(ConflictException.codeTaken(ApiResources.PIZZA_TYPE, "pepperoni"));

        mockMvc.perform(post("/api/pizza-types").contentType(MediaType.APPLICATION_JSON).content("""
                        {"code": "pepperoni", "name": "The Pepperoni Pizza", "category": "Classic", "ingredients": ["Pepperoni"]}
                        """))
                .andExpect(status().isConflict())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Conflict"))
                .andExpect(jsonPath("$.detail").value("Pizza type code 'pepperoni' is already in use"));
    }

    @Test
    @DisplayName("PUT /api/pizza-types/{id} replaces the pizza type, ignoring a code in the body")
    void replace_returnsReplacedPizzaType() throws Exception {
        PizzaTypeUpdateRequest request = new PizzaTypeUpdateRequest("The Pepperoni Pizza", "Classic", List.of("Pepperoni"));
        when(pizzaTypeService.replace(1L, request)).thenReturn(PEPPERONI);

        mockMvc.perform(put("/api/pizza-types/1").contentType(MediaType.APPLICATION_JSON).content("""
                        {"code": "renamed", "name": "The Pepperoni Pizza", "category": "Classic", "ingredients": ["Pepperoni"]}
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("pepperoni"));
        verify(pizzaTypeService).replace(1L, request);
    }

    @Test
    @DisplayName("PUT /api/pizza-types/{id} returns 404 problem detail when the pizza type does not exist")
    void replace_returnsNotFound() throws Exception {
        when(pizzaTypeService.replace(any(), any())).thenThrow(new ResourceNotFoundException(ApiResources.PIZZA_TYPE, 99L));

        mockMvc.perform(put("/api/pizza-types/99").contentType(MediaType.APPLICATION_JSON).content("""
                        {"name": "The Pepperoni Pizza", "category": "Classic", "ingredients": ["Pepperoni"]}
                        """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Pizza type with id 99 not found"));
    }

    @Test
    @DisplayName("DELETE /api/pizza-types/{id} returns 204 with no body")
    void delete_returnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/pizza-types/1")).andExpect(status().isNoContent()).andExpect(content().string(""));

        verify(pizzaTypeService).delete(1L);
    }

    @Test
    @DisplayName("DELETE /api/pizza-types/{id} returns 409 problem detail when the pizza type still has pizzas")
    void delete_returnsConflictWhenPizzaTypeHasPizzas() throws Exception {
        doThrow(ConflictException.stillHasPizzas(1L)).when(pizzaTypeService).delete(1L);

        mockMvc.perform(delete("/api/pizza-types/1"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Pizza type with id 1 still has pizzas; delete them first"));
    }
}
