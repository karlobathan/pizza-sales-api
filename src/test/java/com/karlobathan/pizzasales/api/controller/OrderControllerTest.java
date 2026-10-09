package com.karlobathan.pizzasales.api.controller;

import com.karlobathan.pizzasales.api.ApiResources;
import com.karlobathan.pizzasales.api.domain.PizzaSize;
import com.karlobathan.pizzasales.api.dto.OrderItemRequest;
import com.karlobathan.pizzasales.api.dto.OrderItemResponse;
import com.karlobathan.pizzasales.api.dto.OrderRequest;
import com.karlobathan.pizzasales.api.dto.OrderResponse;
import com.karlobathan.pizzasales.api.dto.OrderSummaryResponse;
import com.karlobathan.pizzasales.api.dto.PageResponse;
import com.karlobathan.pizzasales.api.dto.PizzaResponse;
import com.karlobathan.pizzasales.api.dto.PizzaTypeSummary;
import com.karlobathan.pizzasales.api.exception.InvalidRequestException;
import com.karlobathan.pizzasales.api.exception.ResourceNotFoundException;
import com.karlobathan.pizzasales.api.service.OrderService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrderController.class)
class OrderControllerTest {

    private static final OrderSummaryResponse SUMMARY = new OrderSummaryResponse(1L,
            LocalDate.of(2015, 1, 1),
            LocalTime.of(11, 38, 36)
    );

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrderService orderService;

    @Test
    @DisplayName("GET /api/orders defaults to the first page of 20 with no date range")
    void findAll_defaultsToFirstPageOf20() throws Exception {
        when(orderService.findAll(null, null, 0, 20)).thenReturn(new PageResponse<>(List.of(SUMMARY), 0, 20, 1, 1));

        mockMvc.perform(get("/api/orders"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().json("""
                        {
                          "content": [{"id": 1, "orderDate": "2015-01-01", "orderTime": "11:38:36"}],
                          "page": 0,
                          "size": 20,
                          "totalElements": 1,
                          "totalPages": 1
                        }
                        """, true));
    }

    @Test
    @DisplayName("GET /api/orders passes the date range and paging parameters to the service")
    void findAll_passesDateRangeAndPaging() throws Exception {
        when(orderService.findAll(any(), any(), anyInt(), anyInt())).thenReturn(new PageResponse<>(List.of(), 3, 50, 0, 0));

        mockMvc.perform(get("/api/orders").param("from", "2015-01-01").param("to", "2015-01-31").param("page", "3")
                .param("size", "50")).andExpect(status().isOk());

        verify(orderService).findAll(LocalDate.of(2015, 1, 1), LocalDate.of(2015, 1, 31), 3, 50);
    }

    @Test
    @DisplayName("GET /api/orders returns 400 problem detail when the service rejects the date range")
    void findAll_returnsBadRequestWhenDateRangeIsInvalid() throws Exception {
        LocalDate from = LocalDate.of(2015, 12, 31);
        LocalDate to = LocalDate.of(2015, 1, 1);
        when(orderService.findAll(from, to, 0, 20)).thenThrow(InvalidRequestException.fromAfterTo(from, to));

        mockMvc.perform(get("/api/orders").param("from", "2015-12-31").param("to", "2015-01-01"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Invalid request"))
                .andExpect(jsonPath("$.detail").value("'from' (2015-12-31) must not be after 'to' (2015-01-01)"));
    }

    @Test
    @DisplayName("GET /api/orders returns 400 problem detail for out of range or malformed parameters")
    void findAll_returnsBadRequestForInvalidParameters() throws Exception {
        for (String[] param : List.of(new String[]{"size", "0"},
                new String[]{"size", "101"},
                new String[]{"page", "-1"},
                new String[]{"from", "2015-13-01"},
                new String[]{"to", "yesterday"}
        )) {
            mockMvc.perform(get("/api/orders").param(param[0], param[1]))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                    .andExpect(jsonPath("$.status").value(400));
        }
        verifyNoInteractions(orderService);
    }

    @Test
    @DisplayName("GET /api/orders accepts the largest allowed page size")
    void findAll_acceptsMaxPageSize() throws Exception {
        when(orderService.findAll(null, null, 0, 100)).thenReturn(new PageResponse<>(List.of(), 0, 100, 0, 0));

        mockMvc.perform(get("/api/orders").param("size", "100")).andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/orders/{id} returns the order with its items and totals")
    void findById_returnsOrderWithItemsAndTotals() throws Exception {
        PizzaResponse pizza = new PizzaResponse(10L,
                "pepperoni_m",
                PizzaSize.M,
                new BigDecimal("12.50"),
                new PizzaTypeSummary(1L, "pepperoni", "The Pepperoni Pizza")
        );
        when(orderService.findById(1L)).thenReturn(new OrderResponse(1L,
                LocalDate.of(2015, 1, 1),
                LocalTime.of(11, 38, 36),
                List.of(new OrderItemResponse(100L, pizza, 2, new BigDecimal("25.00"))),
                2,
                new BigDecimal("25.00")
        ));

        mockMvc.perform(get("/api/orders/1")).andExpect(status().isOk()).andExpect(content().json("""
                {
                  "id": 1,
                  "orderDate": "2015-01-01",
                  "orderTime": "11:38:36",
                  "items": [{
                    "id": 100,
                    "pizza": {
                      "id": 10, "code": "pepperoni_m", "size": "M", "price": 12.50,
                      "pizzaType": {"id": 1, "code": "pepperoni", "name": "The Pepperoni Pizza"}
                    },
                    "quantity": 2,
                    "lineTotal": 25.00
                  }],
                  "totalQuantity": 2,
                  "totalPrice": 25.00
                }
                """, true));
    }

    @Test
    @DisplayName("GET /api/orders/{id} returns 404 problem detail when the order does not exist")
    void findById_returnsNotFoundProblemDetail() throws Exception {
        when(orderService.findById(99L)).thenThrow(new ResourceNotFoundException(ApiResources.ORDER, 99L));

        mockMvc.perform(get("/api/orders/99"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("Order with id 99 not found"));
    }

    @Test
    @DisplayName("GET /api/orders/{id} returns 400 problem detail when the id is not a number")
    void findById_returnsBadRequestWhenIdIsNotANumber() throws Exception {
        mockMvc.perform(get("/api/orders/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
        verifyNoInteractions(orderService);
    }

    private static final String ORDER_JSON = """
            {"orderDate": "2015-03-01", "orderTime": "18:30:00", "items": [{"pizzaId": 10, "quantity": 2}]}
            """;

    private static final OrderRequest ORDER_REQUEST = new OrderRequest(LocalDate.of(2015, 3, 1),
            LocalTime.of(18, 30),
            List.of(new OrderItemRequest(10L, 2))
    );

    @Test
    @DisplayName("POST /api/orders returns 201 with the created order and its location")
    void create_returnsCreatedWithLocation() throws Exception {
        when(orderService.create(ORDER_REQUEST)).thenReturn(new OrderResponse(7L,
                LocalDate.of(2015, 3, 1),
                LocalTime.of(18, 30),
                List.of(),
                0,
                BigDecimal.ZERO
        ));

        mockMvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON).content(ORDER_JSON))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/orders/7"))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.orderDate").value("2015-03-01"))
                .andExpect(jsonPath("$.orderTime").value("18:30:00"));
    }

    @Test
    @DisplayName("POST /api/orders returns 400 listing every invalid field without calling the service")
    void create_returnsBadRequestListingInvalidFields() throws Exception {
        mockMvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON).content("""
                        {"orderTime": "18:30:00", "items": [{"quantity": 0}]}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Invalid request"))
                .andExpect(jsonPath("$.detail").value("The request body has invalid fields"))
                .andExpect(content().json("""
                        {"errors": [
                          {"field": "items[0].pizzaId", "message": "must not be null"},
                          {"field": "items[0].quantity", "message": "must be greater than 0"},
                          {"field": "orderDate", "message": "must not be null"}
                        ]}
                        """));
        verifyNoInteractions(orderService);
    }

    @Test
    @DisplayName("POST /api/orders returns 400 for an order with an empty or missing items list")
    void create_returnsBadRequestWithoutItems() throws Exception {
        for (String items : List.of("[]", "null")) {
            mockMvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON).content("""
                            {"orderDate": "2015-03-01", "orderTime": "18:30:00", "items": %s}
                            """.formatted(items)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors", hasSize(1)))
                    .andExpect(jsonPath("$.errors[0].field").value("items"))
                    .andExpect(jsonPath("$.errors[0].message").value("must not be empty"));
        }
        verifyNoInteractions(orderService);
    }

    @Test
    @DisplayName("POST /api/orders returns 400 problem detail for a body that is not valid JSON")
    void create_returnsBadRequestForMalformedJson() throws Exception {
        for (String body : List.of("{\"orderDate\": ", "{\"orderDate\": \"2015-13-45\", \"orderTime\": \"18:30\", \"items\": []}")) {
            mockMvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
        }
        verifyNoInteractions(orderService);
    }

    @Test
    @DisplayName("POST /api/orders returns 400 problem detail when the service rejects unknown pizzas")
    void create_returnsBadRequestForUnknownPizzas() throws Exception {
        when(orderService.create(ORDER_REQUEST)).thenThrow(InvalidRequestException.unknownPizzas(List.of(10L)));

        mockMvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON).content(ORDER_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid request"))
                .andExpect(jsonPath("$.detail").value("Unknown pizza id(s): 10"));
    }
}
