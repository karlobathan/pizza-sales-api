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
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Runs order creation end to end against Postgres, on top of the menu and orders loaded by the real imports:
 * what gets stored, that a rejected order stores nothing, and that re-importing leaves created orders alone.
 */
// same properties as OrderApiIT so both share one cached context and container
@SpringBootTest(properties = {
        "spring.flyway.enabled=true",
        "app.import.pizzas.types-file=classpath:csv/pizza-types.csv",
        "app.import.pizzas.file=classpath:csv/pizzas.csv",
        "app.import.orders.file=classpath:csv/orders.csv",
        "app.import.orders.details-file=classpath:csv/order-details.csv"
})
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class OrderCreateApiIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PizzaImportOrchestrator pizzaImportOrchestrator;

    @Autowired
    private OrderImportOrchestrator orderImportOrchestrator;

    @Autowired
    private JdbcClient jdbcClient;

    private long pepperoniXl;
    private long bbqChickenS;

    @BeforeEach
    void setUp() {
        // sequences are not reset: Hibernate caches blocks of ids across tests, and a reset sequence would hand out
        // ids below the cached ones, so rows would no longer be ordered by id in the order they were inserted
        jdbcClient.sql("""
                TRUNCATE order_item, orders, pizza, pizza_type_ingredient, pizza_type, pizza_ingredient, pizza_category
                CASCADE
                """).update();
        pizzaImportOrchestrator.run();
        orderImportOrchestrator.run();
        pepperoniXl = pizzaId("pepperoni_xl");
        bbqChickenS = pizzaId("bbq_ckn_s");
    }

    @Test
    @DisplayName("POST /api/orders stores the order and its items and returns them with totals")
    void create_storesOrderAndItems() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON).content("""
                        {"orderDate": "2016-02-01", "orderTime": "19:05:00",
                         "items": [{"pizzaId": %d, "quantity": 2}, {"pizzaId": %d, "quantity": 1}]}
                        """.formatted(pepperoniXl, bbqChickenS)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderDate").value("2016-02-01"))
                .andExpect(jsonPath("$.orderTime").value("19:05:00"))
                .andExpect(jsonPath("$.items[*].pizza.code", contains("pepperoni_xl", "bbq_ckn_s")))
                .andExpect(jsonPath("$.items[*].pizza.pizzaType.code", contains("pepperoni", "bbq_ckn")))
                .andExpect(jsonPath("$.items[*].quantity", contains(2, 1)))
                .andExpect(jsonPath("$.items[*].lineTotal", contains(51.00, 12.75)))
                .andExpect(jsonPath("$.totalQuantity").value(3))
                .andExpect(jsonPath("$.totalPrice").value(63.75))
                .andReturn();
        long id = JsonPath.<Number>read(result.getResponse().getContentAsString(), "$.id").longValue();

        mockMvc.perform(get(result.getResponse().getHeader("Location")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.items[*].pizza.code", contains("pepperoni_xl", "bbq_ckn_s")))
                .andExpect(jsonPath("$.totalPrice").value(63.75));
        assertThat(jdbcClient.sql("SELECT source_order_id FROM orders WHERE id = :id")
                .param("id", id)
                .query(Long.class)
                .optional()).isEmpty();
        assertThat(jdbcClient.sql("SELECT COUNT(*) FROM order_item WHERE order_id = :id AND source_order_details_id IS NULL")
                .param("id", id)
                .query(Long.class)
                .single()).isEqualTo(2);
        assertThat(count("orders")).isEqualTo(6);
        assertThat(count("order_item")).isEqualTo(9);
    }

    @Test
    @DisplayName("POST /api/orders with an unknown pizza returns 400 and stores nothing")
    void create_withUnknownPizzaStoresNothing() throws Exception {
        mockMvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON).content("""
                        {"orderDate": "2016-02-01", "orderTime": "19:05:00",
                         "items": [{"pizzaId": %d, "quantity": 1}, {"pizzaId": 999999, "quantity": 1}]}
                        """.formatted(pepperoniXl)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Unknown pizza id(s): 999999"));

        assertThat(count("orders")).isEqualTo(5);
        assertThat(count("order_item")).isEqualTo(7);
    }

    @Test
    @DisplayName("POST /api/orders with invalid fields returns 400 listing them and stores nothing")
    void create_withInvalidFieldsStoresNothing() throws Exception {
        mockMvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON).content("""
                        {"orderDate": "2016-02-01", "items": [{"pizzaId": %d, "quantity": 0}]}
                        """.formatted(pepperoniXl)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[*].field", contains("items[0].quantity", "orderTime")));

        assertThat(count("orders")).isEqualTo(5);
    }

    @Test
    @DisplayName("created orders show up in the order search")
    void create_showsUpInSearch() throws Exception {
        mockMvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON).content("""
                        {"orderDate": "2016-02-01", "orderTime": "19:05:00", "items": [{"pizzaId": %d, "quantity": 1}]}
                        """.formatted(pepperoniXl)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/orders").param("from", "2016-01-01"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].orderDate").value("2016-02-01"));
    }

    @Test
    @DisplayName("re-running the order import leaves orders created through the API alone")
    void reimport_leavesCreatedOrdersAlone() throws Exception {
        mockMvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON).content("""
                        {"orderDate": "2016-02-01", "orderTime": "19:05:00", "items": [{"pizzaId": %d, "quantity": 1}]}
                        """.formatted(pepperoniXl)))
                .andExpect(status().isCreated());

        orderImportOrchestrator.run();

        assertThat(count("orders")).isEqualTo(6);
        assertThat(count("order_item")).isEqualTo(8);
    }

    @Test
    @DisplayName("OpenAPI create order error example matches the problem detail the endpoint actually returns")
    void apiDocs_createErrorExampleMatchesActualResponse() throws Exception {
        String actual = mockMvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON).content("""
                        {"orderDate": "2016-02-01", "orderTime": "19:05:00", "items": [{"pizzaId": 99, "quantity": 1}]}
                        """))
                .andExpect(status().isBadRequest())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String documented = "$.components.responses.OrderCreateBadRequest.content['application/problem+json']";
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(jsonPath("$.paths['/api/orders'].post.responses['400']['$ref']")
                        .value("#/components/responses/OrderCreateBadRequest"))
                .andExpect(jsonPath("$.paths['/api/orders'].post.responses['201'].content['application/json'].schema['$ref']")
                        .exists())
                .andExpect(jsonPath(documented + ".schema['$ref']").value("#/components/schemas/ProblemDetail"))
                .andExpect(jsonPath(documented + ".example").value(JsonPath.<Object>read(actual, "$")));
    }

    private long pizzaId(String code) {
        return jdbcClient.sql("SELECT id FROM pizza WHERE code = :code").param("code", code).query(Long.class).single();
    }

    private long count(String table) {
        return jdbcClient.sql("SELECT COUNT(*) FROM " + table).query(Long.class).single();
    }
}
