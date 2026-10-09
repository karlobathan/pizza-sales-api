package com.karlobathan.pizzasales.api.controller;

import com.karlobathan.pizzasales.api.ApiResources;
import com.karlobathan.pizzasales.api.domain.PizzaSize;
import com.karlobathan.pizzasales.api.dto.PizzaResponse;
import com.karlobathan.pizzasales.api.dto.PizzaTypeSummary;
import com.karlobathan.pizzasales.api.exception.ResourceNotFoundException;
import com.karlobathan.pizzasales.api.service.PizzaService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PizzaController.class)
class PizzaControllerTest {

    private static final PizzaTypeSummary PEPPERONI_TYPE = new PizzaTypeSummary(1L, "pepperoni", "The Pepperoni Pizza");
    private static final PizzaResponse    PEPPERONI_M    = new PizzaResponse(10L,
            "pepperoni_m",
            PizzaSize.M,
            new BigDecimal("12.50"),
            PEPPERONI_TYPE
    );

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PizzaService pizzaService;

    @Test
    @DisplayName("GET /api/pizzas returns every pizza")
    void findAll_returnsEveryPizza() throws Exception {
        PizzaResponse pepperoniL = new PizzaResponse(11L, "pepperoni_l", PizzaSize.L, new BigDecimal("15.25"), PEPPERONI_TYPE);
        when(pizzaService.findAll()).thenReturn(List.of(PEPPERONI_M, pepperoniL));

        mockMvc.perform(get("/api/pizzas"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].code", contains("pepperoni_m", "pepperoni_l")))
                .andExpect(jsonPath("$[*].size", contains("M", "L")));
    }

    @Test
    @DisplayName("GET /api/pizzas returns an empty list when there are none")
    void findAll_returnsEmptyList() throws Exception {
        when(pizzaService.findAll()).thenReturn(List.of());

        mockMvc.perform(get("/api/pizzas")).andExpect(status().isOk()).andExpect(content().json("[]"));
    }

    @Test
    @DisplayName("GET /api/pizzas/{id} returns the pizza with size, price and pizza type")
    void findById_returnsPizza() throws Exception {
        when(pizzaService.findById(10L)).thenReturn(PEPPERONI_M);

        mockMvc.perform(get("/api/pizzas/10")).andExpect(status().isOk()).andExpect(content().json("""
                {
                  "id": 10,
                  "code": "pepperoni_m",
                  "size": "M",
                  "price": 12.50,
                  "pizzaType": {"id": 1, "code": "pepperoni", "name": "The Pepperoni Pizza"}
                }
                """, true));
    }

    @Test
    @DisplayName("GET /api/pizzas/{id} returns 404 problem detail when the pizza does not exist")
    void findById_returnsNotFoundProblemDetail() throws Exception {
        when(pizzaService.findById(99L)).thenThrow(new ResourceNotFoundException(ApiResources.PIZZA, 99L));

        mockMvc.perform(get("/api/pizzas/99"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value("Pizza with id 99 not found"));
    }

    @Test
    @DisplayName("GET /api/pizzas/{id} returns 400 problem detail when the id is not a number")
    void findById_returnsBadRequestWhenIdIsNotANumber() throws Exception {
        mockMvc.perform(get("/api/pizzas/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
        verifyNoInteractions(pizzaService);
    }
}
