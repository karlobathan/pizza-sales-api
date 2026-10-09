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
import org.springframework.test.web.servlet.ResultActions;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Runs the pizza write endpoints end to end against Postgres, on top of the menu and orders loaded by the real
 * imports: codes and sizes checked for conflicts, a deleted pizza hidden from the menu and new orders but still shown
 * on the orders placed with it, a deleted size free to be created again, and docs examples.
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
class PizzaWriteApiIT {

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
        // sequences are not reset: Hibernate caches blocks of ids across tests, and a reset sequence would hand out
        // ids below the cached ones, so rows would no longer be ordered by id in the order they were inserted
        jdbcClient.sql("""
                TRUNCATE order_item, orders, pizza, pizza_type_ingredient, pizza_type, pizza_ingredient, pizza_category
                CASCADE
                """).update();
        pizzaImportOrchestrator.run();
        orderImportOrchestrator.run();
    }

    @Test
    @DisplayName("POST /api/pizzas creates a new size of a pizza type")
    void create_createsNewSize() throws Exception {
        MvcResult result = createPizza("pepperoni_m", idOf("pizza_type", "pepperoni"), "M", "12.50")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("pepperoni_m"))
                .andExpect(jsonPath("$.size").value("M"))
                .andExpect(jsonPath("$.price").value(12.50))
                .andExpect(jsonPath("$.pizzaType.code").value("pepperoni"))
                .andReturn();

        mockMvc.perform(get(result.getResponse().getHeader("Location"))).andExpect(jsonPath("$.code").value("pepperoni_m"));
        mockMvc.perform(get("/api/pizzas")).andExpect(jsonPath("$[*].code", hasItem("pepperoni_m")));
    }

    @Test
    @DisplayName("POST /api/pizzas returns 409 for a used code or a size the pizza type already has")
    void create_returnsConflictForUsedCodeOrSize() throws Exception {
        long pepperoni = idOf("pizza_type", "pepperoni");

        createPizza("pepperoni_xl", pepperoni, "M", "12.50").andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Pizza code 'pepperoni_xl' is already in use"));
        createPizza("pepperoni_xl2", pepperoni, "XL", "12.50").andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Pizza type 'pepperoni' already has a size XL pizza"));
        assertThat(count("pizza")).isEqualTo(6);
    }

    @Test
    @DisplayName("POST /api/pizzas returns 400 for an unknown or deleted pizza type")
    void create_returnsBadRequestForUnknownOrDeletedPizzaType() throws Exception {
        long nutella = idFrom(mockMvc.perform(post("/api/pizza-types").contentType(MediaType.APPLICATION_JSON).content("""
                        {"code": "nutella", "name": "The Nutella Pizza", "category": "Dessert", "ingredients": ["Nutella"]}
                        """)).andReturn());
        mockMvc.perform(delete("/api/pizza-types/{id}", nutella)).andExpect(status().isNoContent());

        for (long pizzaTypeId : List.of(999999L, nutella)) {
            createPizza("ghost_m", pizzaTypeId, "M", "12.50").andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.detail").value("Unknown pizza type id: " + pizzaTypeId));
        }
    }

    @Test
    @DisplayName("PUT /api/pizzas/{id} changes the price, and orders with the pizza show totals at the new price")
    void replace_changesPriceAndOrderTotals() throws Exception {
        long pizza = idOf("pizza", "pepperoni_xl");
        long order = jdbcClient.sql("SELECT id FROM orders WHERE source_order_id = 2").query(Long.class).single();
        mockMvc.perform(get("/api/orders/{id}", order)).andExpect(jsonPath("$.totalPrice").value(59.00));

        mockMvc.perform(put("/api/pizzas/{id}", pizza).contentType(MediaType.APPLICATION_JSON).content("""
                        {"pizzaTypeId": %d, "size": "XL", "price": 30.00}
                        """.formatted(idOf("pizza_type", "pepperoni"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("pepperoni_xl"))
                .andExpect(jsonPath("$.price").value(30.00));

        mockMvc.perform(get("/api/orders/{id}", order)).andExpect(jsonPath("$.totalPrice").value(63.50));
    }

    @Test
    @DisplayName("PUT /api/pizzas/{id} returns 409 for a size another pizza of the pizza type has, and allows its own size")
    void replace_checksSizeAgainstOtherPizzasOnly() throws Exception {
        long pizza = idOf("pizza", "pepperoni_xl");
        long pepperoni = idOf("pizza_type", "pepperoni");

        replacePizza(pizza, pepperoni, "XXL", "30.00").andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Pizza type 'pepperoni' already has a size XXL pizza"));
        replacePizza(pizza, pepperoni, "XL", "30.00").andExpect(status().isOk());
    }

    @Test
    @DisplayName("DELETE /api/pizzas/{id} hides the pizza from the menu and new orders but not from past orders")
    void delete_hidesPizzaButNotFromPastOrders() throws Exception {
        long pizza = idOf("pizza", "pepperoni_xl");
        long order = jdbcClient.sql("SELECT id FROM orders WHERE source_order_id = 2").query(Long.class).single();

        mockMvc.perform(delete("/api/pizzas/{id}", pizza)).andExpect(status().isNoContent());

        mockMvc.perform(get("/api/pizzas/{id}", pizza)).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/pizzas")).andExpect(jsonPath("$[*].code", not(hasItem("pepperoni_xl"))));
        mockMvc.perform(delete("/api/pizzas/{id}", pizza)).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/orders/{id}", order))
                .andExpect(jsonPath("$.items[*].pizza.code", contains("pepperoni_xl", "ckn_pesto_m")))
                .andExpect(jsonPath("$.totalPrice").value(59.00));
        mockMvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON).content("""
                        {"orderDate": "2016-02-01", "orderTime": "19:05:00", "items": [{"pizzaId": %d, "quantity": 1}]}
                        """.formatted(pizza)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Unknown pizza id(s): " + pizza));
        assertThat(isDeleted("pizza", "pepperoni_xl")).isTrue();
    }

    @Test
    @DisplayName("a deleted pizza stays deleted after a re-import, and its size can be created again under a new code")
    void delete_survivesReimportAndFreesSize() throws Exception {
        long pepperoni = idOf("pizza_type", "pepperoni");
        mockMvc.perform(delete("/api/pizzas/{id}", idOf("pizza", "pepperoni_xl"))).andExpect(status().isNoContent());

        pizzaImportOrchestrator.run();

        assertThat(count("pizza")).isEqualTo(6);
        assertThat(isDeleted("pizza", "pepperoni_xl")).isTrue();
        createPizza("pepperoni_xl", pepperoni, "XL", "26.00").andExpect(status().isConflict());
        createPizza("pepperoni_xl_v2", pepperoni, "XL", "26.00").andExpect(status().isCreated());
    }

    @Test
    @DisplayName("OpenAPI docs point the pizza writes to their error responses, whose examples match what they return")
    void apiDocs_pizzaWriteErrorsMatchActualResponses() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(jsonPath("$.paths['/api/pizzas'].post.responses['400']['$ref']").value("#/components/responses/PizzaWriteBadRequest"))
                .andExpect(jsonPath("$.paths['/api/pizzas'].post.responses['409']['$ref']").value("#/components/responses/PizzaConflict"))
                .andExpect(jsonPath("$.paths['/api/pizzas/{id}'].put.responses['409']['$ref']").value("#/components/responses/PizzaConflict"))
                .andExpect(jsonPath("$.paths['/api/pizzas/{id}'].delete.responses['404']['$ref']").value("#/components/responses/PizzaNotFound"));

        String badRequest = createPizza("ghost_m", 99L, "M", "12.50").andReturn().getResponse().getContentAsString();
        String conflict = createPizza("pepperoni_xl", idOf("pizza_type", "pepperoni"), "M", "12.50")
                .andReturn()
                .getResponse()
                .getContentAsString();
        String examples = "$.components.responses.%s.content['application/problem+json'].example";
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(jsonPath(examples.formatted("PizzaWriteBadRequest")).value(json(badRequest)))
                .andExpect(jsonPath(examples.formatted("PizzaConflict")).value(json(conflict)));
    }

    private ResultActions createPizza(String code, long pizzaTypeId, String size, String price)
            throws Exception {
        return mockMvc.perform(post("/api/pizzas").contentType(MediaType.APPLICATION_JSON).content("""
                {"code": "%s", "pizzaTypeId": %d, "size": "%s", "price": %s}
                """.formatted(code, pizzaTypeId, size, price)));
    }

    private ResultActions replacePizza(long id, long pizzaTypeId, String size, String price)
            throws Exception {
        return mockMvc.perform(put("/api/pizzas/{id}", id).contentType(MediaType.APPLICATION_JSON).content("""
                {"pizzaTypeId": %d, "size": "%s", "price": %s}
                """.formatted(pizzaTypeId, size, price)));
    }

    private long idOf(String table, String code) {
        return jdbcClient.sql("SELECT id FROM " + table + " WHERE code = :code").param("code", code).query(Long.class).single();
    }

    private boolean isDeleted(String table, String code) {
        return jdbcClient.sql("SELECT deleted_at IS NOT NULL FROM " + table + " WHERE code = :code")
                .param("code", code)
                .query(Boolean.class)
                .single();
    }

    private long count(String table) {
        return jdbcClient.sql("SELECT COUNT(*) FROM " + table).query(Long.class).single();
    }

    private long idFrom(MvcResult result) throws Exception {
        return JsonPath.<Number>read(result.getResponse().getContentAsString(), "$.id").longValue();
    }

    private Object json(String content) {
        return JsonPath.read(content, "$");
    }
}

