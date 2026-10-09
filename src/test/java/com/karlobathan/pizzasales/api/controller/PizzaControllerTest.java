package com.karlobathan.pizzasales.api.controller;

import com.karlobathan.pizzasales.api.ApiResources;
import com.karlobathan.pizzasales.api.domain.PizzaSize;
import com.karlobathan.pizzasales.api.dto.PizzaCreateRequest;
import com.karlobathan.pizzasales.api.dto.PizzaResponse;
import com.karlobathan.pizzasales.api.dto.PizzaTypeSummary;
import com.karlobathan.pizzasales.api.dto.PizzaUpdateRequest;
import com.karlobathan.pizzasales.api.exception.ConflictException;
import com.karlobathan.pizzasales.api.exception.InvalidRequestException;
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

    @Test
    @DisplayName("POST /api/pizzas returns 201 with the created pizza and its location")
    void create_returnsCreatedWithLocation() throws Exception {
        when(pizzaService.create(new PizzaCreateRequest("pepperoni_m", 1L, PizzaSize.M, new BigDecimal("12.50"))))
                .thenReturn(PEPPERONI_M);

        mockMvc.perform(post("/api/pizzas").contentType(MediaType.APPLICATION_JSON).content("""
                        {"code": "pepperoni_m", "pizzaTypeId": 1, "size": "M", "price": 12.50}
                        """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/pizzas/10"))
                .andExpect(jsonPath("$.code").value("pepperoni_m"));
    }

    @Test
    @DisplayName("POST /api/pizzas returns 400 listing every invalid field without calling the service")
    void create_returnsBadRequestListingInvalidFields() throws Exception {
        mockMvc.perform(post("/api/pizzas").contentType(MediaType.APPLICATION_JSON).content("""
                        {"code": "pepperoni_m", "size": "M", "price": -1.005}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(content().json("""
                        {"errors": [
                          {"field": "pizzaTypeId", "message": "must not be null"},
                          {"field": "price", "message": "must be greater than or equal to 0.00"},
                          {"field": "price", "message": "numeric value out of bounds (<4 digits>.<2 digits> expected)"}
                        ]}
                        """));
        verifyNoInteractions(pizzaService);
    }

    @Test
    @DisplayName("POST /api/pizzas returns 400 problem detail for a size that doesn't exist")
    void create_returnsBadRequestForUnknownSize() throws Exception {
        mockMvc.perform(post("/api/pizzas").contentType(MediaType.APPLICATION_JSON).content("""
                        {"code": "pepperoni_m", "pizzaTypeId": 1, "size": "XXXL", "price": 12.50}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
        verifyNoInteractions(pizzaService);
    }

    @Test
    @DisplayName("POST /api/pizzas returns 400 when the service rejects the pizza type and 409 when the code is taken")
    void create_returnsServiceErrors() throws Exception {
        String body = """
                {"code": "pepperoni_m", "pizzaTypeId": 99, "size": "M", "price": 12.50}
                """;
        when(pizzaService.create(any())).thenThrow(InvalidRequestException.unknownPizzaType(99L))
                .thenThrow(ConflictException.codeTaken(ApiResources.PIZZA, "pepperoni_m"));

        mockMvc.perform(post("/api/pizzas").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Unknown pizza type id: 99"));
        mockMvc.perform(post("/api/pizzas").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Pizza code 'pepperoni_m' is already in use"));
    }

    @Test
    @DisplayName("PUT /api/pizzas/{id} replaces the pizza and returns it")
    void replace_returnsReplacedPizza() throws Exception {
        PizzaUpdateRequest request = new PizzaUpdateRequest(1L, PizzaSize.L, new BigDecimal("15.25"));
        when(pizzaService.replace(10L, request)).thenReturn(PEPPERONI_M);

        mockMvc.perform(put("/api/pizzas/10").contentType(MediaType.APPLICATION_JSON).content("""
                        {"pizzaTypeId": 1, "size": "L", "price": 15.25}
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10));
        verify(pizzaService).replace(10L, request);
    }

    @Test
    @DisplayName("PUT /api/pizzas/{id} returns 409 problem detail when the size is taken")
    void replace_returnsConflictWhenSizeIsTaken() throws Exception {
        when(pizzaService.replace(any(), any())).thenThrow(ConflictException.sizeTaken("pepperoni", PizzaSize.L));

        mockMvc.perform(put("/api/pizzas/10").contentType(MediaType.APPLICATION_JSON).content("""
                        {"pizzaTypeId": 1, "size": "L", "price": 15.25}
                        """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Pizza type 'pepperoni' already has a size L pizza"));
    }

    @Test
    @DisplayName("DELETE /api/pizzas/{id} returns 204 with no body and 404 for an unknown pizza")
    void delete_returnsNoContentOrNotFound() throws Exception {
        doThrow(new ResourceNotFoundException(ApiResources.PIZZA, 99L)).when(pizzaService).delete(99L);

        mockMvc.perform(delete("/api/pizzas/10")).andExpect(status().isNoContent()).andExpect(content().string(""));
        mockMvc.perform(delete("/api/pizzas/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Pizza with id 99 not found"));
        verify(pizzaService).delete(10L);
    }
}
