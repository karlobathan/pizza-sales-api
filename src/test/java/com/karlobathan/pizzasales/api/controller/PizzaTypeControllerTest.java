package com.karlobathan.pizzasales.api.controller;

import com.karlobathan.pizzasales.api.ApiResources;
import com.karlobathan.pizzasales.api.dto.PizzaTypeResponse;
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
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
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
}
