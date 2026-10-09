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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Runs order replacement end to end against Postgres, on top of the menu and orders loaded by the real imports:
 * the new date, time and items are what the API returns afterwards, the replaced items are soft-deleted (kept with
 * their source ids, so a re-import doesn't add them back), and a rejected replacement changes nothing.
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
class OrderReplaceApiIT {

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
    @DisplayName("PUT /api/orders/{id} replaces an imported order's date, time and items")
    void replace_replacesImportedOrder() throws Exception {
        long id = importedOrderId(2);

        mockMvc.perform(put("/api/orders/{id}", id).contentType(MediaType.APPLICATION_JSON).content("""
                        {"orderDate": "2015-01-03", "orderTime": "10:00:00", "items": [{"pizzaId": %d, "quantity": 4}]}
                        """.formatted(bbqChickenS)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.orderDate").value("2015-01-03"))
                .andExpect(jsonPath("$.orderTime").value("10:00:00"))
                .andExpect(jsonPath("$.items[*].pizza.code", contains("bbq_ckn_s")))
                .andExpect(jsonPath("$.items[*].quantity", contains(4)))
                .andExpect(jsonPath("$.totalQuantity").value(4))
                .andExpect(jsonPath("$.totalPrice").value(51.00));

        mockMvc.perform(get("/api/orders/{id}", id))
                .andExpect(jsonPath("$.orderDate").value("2015-01-03"))
                .andExpect(jsonPath("$.items[*].pizza.code", contains("bbq_ckn_s")))
                .andExpect(jsonPath("$.totalPrice").value(51.00));
        mockMvc.perform(get("/api/orders").param("from", "2015-01-03").param("to", "2015-01-03"))
                .andExpect(jsonPath("$.content[*].id", contains((int) id)));
    }

    @Test
    @DisplayName("PUT /api/orders/{id} keeps the replaced items in the database marked deleted, with their source ids")
    void replace_softDeletesReplacedItems() throws Exception {
        long id = importedOrderId(2);

        replace(id, bbqChickenS, 4);

        assertThat(jdbcClient.sql("""
                SELECT source_order_details_id FROM order_item
                WHERE order_id = :id AND deleted_at IS NOT NULL
                ORDER BY source_order_details_id
                """).param("id", id).query(Long.class).list()).containsExactly(2L, 3L);
        assertThat(jdbcClient.sql("""
                SELECT COUNT(*) FROM order_item
                WHERE order_id = :id AND deleted_at IS NULL AND source_order_details_id IS NULL
                """).param("id", id).query(Long.class).single()).isEqualTo(1);
        assertThat(count("order_item")).isEqualTo(8);
    }

    @Test
    @DisplayName("re-running both imports doesn't add the replaced items back")
    void reimport_doesNotRestoreReplacedItems() throws Exception {
        long id = importedOrderId(2);
        replace(id, bbqChickenS, 4);

        pizzaImportOrchestrator.run();
        orderImportOrchestrator.run();

        assertThat(count("order_item")).isEqualTo(8);
        mockMvc.perform(get("/api/orders/{id}", id))
                .andExpect(jsonPath("$.items[*].pizza.code", contains("bbq_ckn_s")))
                .andExpect(jsonPath("$.totalQuantity").value(4));
    }

    @Test
    @DisplayName("PUT /api/orders/{id} can be repeated, soft-deleting the previous replacement each time")
    void replace_canBeRepeated() throws Exception {
        long id = importedOrderId(2);

        replace(id, bbqChickenS, 4);
        replace(id, pepperoniXl, 1);

        mockMvc.perform(get("/api/orders/{id}", id))
                .andExpect(jsonPath("$.items[*].pizza.code", contains("pepperoni_xl")))
                .andExpect(jsonPath("$.totalQuantity").value(1));
        assertThat(jdbcClient.sql("SELECT COUNT(*) FROM order_item WHERE order_id = :id AND deleted_at IS NOT NULL")
                .param("id", id)
                .query(Long.class)
                .single()).isEqualTo(3);
    }

    @Test
    @DisplayName("PUT /api/orders/{id} replaces an order created through the API")
    void replace_replacesCreatedOrder() throws Exception {
        String created = mockMvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON).content("""
                        {"orderDate": "2016-02-01", "orderTime": "19:05:00", "items": [{"pizzaId": %d, "quantity": 2}]}
                        """.formatted(pepperoniXl)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        long id = JsonPath.<Number>read(created, "$.id").longValue();

        replace(id, bbqChickenS, 1);

        mockMvc.perform(get("/api/orders/{id}", id))
                .andExpect(jsonPath("$.orderDate").value("2015-01-03"))
                .andExpect(jsonPath("$.items[*].pizza.code", contains("bbq_ckn_s")));
    }

    @Test
    @DisplayName("PUT /api/orders/{id} with an unknown pizza returns 400 and leaves the order and its items as they were")
    void replace_withUnknownPizzaChangesNothing() throws Exception {
        long id = importedOrderId(2);

        mockMvc.perform(put("/api/orders/{id}", id).contentType(MediaType.APPLICATION_JSON).content("""
                        {"orderDate": "2015-01-03", "orderTime": "10:00:00",
                         "items": [{"pizzaId": %d, "quantity": 1}, {"pizzaId": 999999, "quantity": 1}]}
                        """.formatted(bbqChickenS)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Unknown pizza id(s): 999999"));

        mockMvc.perform(get("/api/orders/{id}", id))
                .andExpect(jsonPath("$.orderDate").value("2015-01-01"))
                .andExpect(jsonPath("$.orderTime").value("11:57:40"))
                .andExpect(jsonPath("$.items[*].pizza.code", contains("pepperoni_xl", "ckn_pesto_m")));
        assertThat(count("order_item")).isEqualTo(7);
        assertThat(jdbcClient.sql("SELECT COUNT(*) FROM order_item WHERE deleted_at IS NOT NULL").query(Long.class).single())
                .isZero();
    }

    @Test
    @DisplayName("PUT /api/orders/{id} returns 404 for an unknown or deleted order")
    void replace_returnsNotFoundForUnknownOrDeletedOrder() throws Exception {
        long deleted = importedOrderId(1);
        mockMvc.perform(delete("/api/orders/{id}", deleted)).andExpect(status().isNoContent());

        for (long id : List.of(999999L, deleted)) {
            mockMvc.perform(put("/api/orders/{id}", id).contentType(MediaType.APPLICATION_JSON).content("""
                            {"orderDate": "2015-01-03", "orderTime": "10:00:00", "items": [{"pizzaId": %d, "quantity": 1}]}
                            """.formatted(bbqChickenS)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.detail").value("Order with id " + id + " not found"));
        }
        assertThat(itemCount(deleted)).isEqualTo(1);
    }

    @Test
    @DisplayName("OpenAPI docs point PUT to the order not found and shared write bad request responses")
    void apiDocs_pointPutToItsErrorResponses() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(jsonPath("$.paths['/api/orders/{id}'].put.responses['404']['$ref']")
                        .value("#/components/responses/OrderNotFound"))
                .andExpect(jsonPath("$.paths['/api/orders/{id}'].put.responses['400']['$ref']")
                        .value("#/components/responses/OrderWriteBadRequest"))
                .andExpect(jsonPath("$.paths['/api/orders/{id}'].put.responses['200'].content['application/json'].schema['$ref']")
                        .exists());
    }

    private void replace(long orderId, long pizzaId, int quantity) throws Exception {
        mockMvc.perform(put("/api/orders/{id}", orderId).contentType(MediaType.APPLICATION_JSON).content("""
                        {"orderDate": "2015-01-03", "orderTime": "10:00:00", "items": [{"pizzaId": %d, "quantity": %d}]}
                        """.formatted(pizzaId, quantity)))
                .andExpect(status().isOk());
    }

    private long importedOrderId(long sourceOrderId) {
        return jdbcClient.sql("SELECT id FROM orders WHERE source_order_id = :id")
                .param("id", sourceOrderId)
                .query(Long.class)
                .single();
    }

    private long pizzaId(String code) {
        return jdbcClient.sql("SELECT id FROM pizza WHERE code = :code").param("code", code).query(Long.class).single();
    }

    private long itemCount(long orderId) {
        return jdbcClient.sql("SELECT COUNT(*) FROM order_item WHERE order_id = :id")
                .param("id", orderId)
                .query(Long.class)
                .single();
    }

    private long count(String table) {
        return jdbcClient.sql("SELECT COUNT(*) FROM " + table).query(Long.class).single();
    }
}
