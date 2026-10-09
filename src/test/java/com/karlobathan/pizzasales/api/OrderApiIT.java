package com.karlobathan.pizzasales.api;

import com.jayway.jsonpath.JsonPath;
import com.karlobathan.pizzasales.TestcontainersConfiguration;
import com.karlobathan.pizzasales.importer.service.OrderImportOrchestrator;
import com.karlobathan.pizzasales.importer.service.PizzaImportOrchestrator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Runs the order endpoints end to end against Postgres, with the menu and orders loaded by the real imports.
 * Covers what the slice tests can't: the date filter and sort in SQL, paging totals, and that each item's
 * pizza and pizza type are fetched (open-in-view is off, so a missed fetch fails with a LazyInitializationException).
 */
@SpringBootTest(properties = {
        "spring.flyway.enabled=true",
        "app.import.pizzas.types-file=classpath:csv/pizza-types.csv",
        "app.import.pizzas.file=classpath:csv/pizzas.csv",
        "app.import.orders.file=classpath:csv/orders.csv",
        "app.import.orders.details-file=classpath:csv/order-details.csv"
})
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class OrderApiIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PizzaImportOrchestrator pizzaImportOrchestrator;

    @Autowired
    private OrderImportOrchestrator orderImportOrchestrator;

    @Autowired
    private JdbcClient jdbcClient;

    @BeforeEach
    void setUp() {
        jdbcClient.sql("""
                TRUNCATE order_item, orders, pizza, pizza_type_ingredient, pizza_type, pizza_ingredient, pizza_category
                RESTART IDENTITY CASCADE
                """).update();
        pizzaImportOrchestrator.run();
        orderImportOrchestrator.run();
    }

    @Test
    @DisplayName("GET /api/orders returns every order sorted by date and time on one page by default")
    void orders_returnsEveryOrderSortedByDateAndTime() throws Exception {
        mockMvc.perform(get("/api/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].id", contains(idsOf(1, 2, 3, 4, 5))))
                .andExpect(jsonPath("$.content[0].orderDate").value("2015-01-01"))
                .andExpect(jsonPath("$.content[0].orderTime").value("11:38:36"))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(5))
                .andExpect(jsonPath("$.totalPages").value(1));
    }

    @Test
    @DisplayName("GET /api/orders filters by an inclusive date range")
    void orders_filtersByInclusiveDateRange() throws Exception {
        mockMvc.perform(get("/api/orders").param("from", "2015-01-01").param("to", "2015-01-02"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].id", contains(idsOf(1, 2, 3))))
                .andExpect(jsonPath("$.totalElements").value(3));
    }

    @Test
    @DisplayName("GET /api/orders filters by only a start or only an end date")
    void orders_filtersByOpenEndedRange() throws Exception {
        mockMvc.perform(get("/api/orders").param("from", "2015-06-15"))
                .andExpect(jsonPath("$.content[*].id", contains(idsOf(4, 5))));
        mockMvc.perform(get("/api/orders").param("to", "2015-01-01"))
                .andExpect(jsonPath("$.content[*].id", contains(idsOf(1, 2))));
    }

    @Test
    @DisplayName("GET /api/orders pages through the results with stable totals")
    void orders_pagesThroughResults() throws Exception {
        mockMvc.perform(get("/api/orders").param("size", "2").param("page", "0"))
                .andExpect(jsonPath("$.content[*].id", contains(idsOf(1, 2))))
                .andExpect(jsonPath("$.totalElements").value(5))
                .andExpect(jsonPath("$.totalPages").value(3));
        mockMvc.perform(get("/api/orders").param("size", "2").param("page", "2"))
                .andExpect(jsonPath("$.content[*].id", contains(idsOf(5))));
        mockMvc.perform(get("/api/orders").param("size", "2").param("page", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", empty()));
    }

    @Test
    @DisplayName("GET /api/orders returns an empty page when no order falls in the range")
    void orders_returnsEmptyPageWhenNothingMatches() throws Exception {
        mockMvc.perform(get("/api/orders").param("from", "2016-01-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", empty()))
                .andExpect(jsonPath("$.totalElements").value(0))
                .andExpect(jsonPath("$.totalPages").value(0));
    }

    @Test
    @DisplayName("GET /api/orders/{id} returns the order with its items, pizzas and totals")
    void order_returnsOrderWithItemsAndTotals() throws Exception {
        int id = idsOf(2)[0];

        mockMvc.perform(get("/api/orders/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.orderDate").value("2015-01-01"))
                .andExpect(jsonPath("$.orderTime").value("11:57:40"))
                .andExpect(jsonPath("$.items[*].pizza.code", contains("pepperoni_xl", "ckn_pesto_m")))
                .andExpect(jsonPath("$.items[*].quantity", contains(1, 2)))
                .andExpect(jsonPath("$.items[*].lineTotal", contains(25.50, 33.50)))
                .andExpect(jsonPath("$.items[0].pizza.pizzaType.code").value("pepperoni"))
                .andExpect(jsonPath("$.totalQuantity").value(3))
                .andExpect(jsonPath("$.totalPrice").value(59.00));
    }

    @Test
    @DisplayName("GET /api/orders/{id} returns 404 for an unknown id")
    void order_returnsNotFoundForUnknownId() throws Exception {
        mockMvc.perform(get("/api/orders/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Order with id 999999 not found"));
    }

    @Test
    @DisplayName("OpenAPI docs point the order endpoints to their own error responses")
    void apiDocs_pointOrderEndpointsToTheirErrorResponses() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/orders'].get.responses['400']['$ref']")
                        .value("#/components/responses/OrderSearchBadRequest"))
                .andExpect(jsonPath("$.paths['/api/orders/{id}'].get.responses['404']['$ref']")
                        .value("#/components/responses/OrderNotFound"))
                .andExpect(jsonPath("$.paths['/api/orders/{id}'].get.responses['400']['$ref']")
                        .value("#/components/responses/OrderBadRequest"));
    }

    @Test
    @DisplayName("OpenAPI order error examples match the problem detail each endpoint actually returns")
    void apiDocs_orderErrorExamplesMatchActualResponses() throws Exception {
        for (String[] endpoint : List.of(new String[]{"/api/orders/99", "OrderNotFound"},
                new String[]{"/api/orders/abc", "OrderBadRequest"},
                new String[]{"/api/orders?from=2015-12-31&to=2015-01-01", "OrderSearchBadRequest"}
        )) {
            String documented = "$.components.responses." + endpoint[1] + ".content['application/problem+json']";
            String actual = mockMvc.perform(get(endpoint[0])).andReturn().getResponse().getContentAsString();

            mockMvc.perform(get("/v3/api-docs"))
                    .andExpect(jsonPath(documented + ".schema['$ref']").value("#/components/schemas/ProblemDetail"))
                    .andExpect(jsonPath(documented + ".example").value(JsonPath.<Object>read(actual, "$")));
        }
    }

    // database ids of the orders with these source order ids, in the same order (Integer, as JSON numbers parse to it)
    private Integer[] idsOf(long... sourceOrderIds) {
        return Arrays.stream(sourceOrderIds)
                .mapToObj(sourceId -> jdbcClient.sql("SELECT id FROM orders WHERE source_order_id = :id")
                        .param("id", sourceId)
                        .query(Integer.class)
                        .single())
                .toArray(Integer[]::new);
    }
}
