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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Runs order deletion end to end against Postgres, on top of the menu and orders loaded by the real imports:
 * a deleted order disappears from the API but stays in the database with its items (soft delete), a re-import
 * doesn't bring it back, other orders are untouched, and errors match the docs.
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
class OrderDeleteApiIT {

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
    @DisplayName("DELETE /api/orders/{id} hides the order but keeps it and its items in the database")
    void delete_hidesOrderButKeepsRows() throws Exception {
        long id = importedOrderId(2);

        mockMvc.perform(delete("/api/orders/{id}", id)).andExpect(status().isNoContent()).andExpect(content().string(""));

        mockMvc.perform(get("/api/orders/{id}", id)).andExpect(status().isNotFound());
        assertThat(isDeleted(id)).isTrue();
        assertThat(itemCount(id)).isEqualTo(2);
        assertThat(count("orders")).isEqualTo(5);
        assertThat(count("order_item")).isEqualTo(7);
        assertThat(isDeleted(importedOrderId(5))).isFalse();
        mockMvc.perform(get("/api/orders/{id}", importedOrderId(5))).andExpect(status().isOk());
    }

    @Test
    @DisplayName("re-running both imports doesn't bring a deleted order back")
    void reimport_doesNotRestoreDeletedOrder() throws Exception {
        long id = importedOrderId(2);
        mockMvc.perform(delete("/api/orders/{id}", id)).andExpect(status().isNoContent());

        pizzaImportOrchestrator.run();
        orderImportOrchestrator.run();

        assertThat(count("orders")).isEqualTo(5);
        assertThat(count("order_item")).isEqualTo(7);
        assertThat(isDeleted(id)).isTrue();
        mockMvc.perform(get("/api/orders/{id}", id)).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/orders")).andExpect(jsonPath("$.totalElements").value(4));
    }

    @Test
    @DisplayName("DELETE /api/orders/{id} deletes an order created through the API")
    void delete_deletesCreatedOrder() throws Exception {
        long pizzaId = jdbcClient.sql("SELECT id FROM pizza WHERE code = 'pepperoni_xl'").query(Long.class).single();
        String created = mockMvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON).content("""
                        {"orderDate": "2016-02-01", "orderTime": "19:05:00", "items": [{"pizzaId": %d, "quantity": 2}]}
                        """.formatted(pizzaId)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        long id = JsonPath.<Number>read(created, "$.id").longValue();

        mockMvc.perform(delete("/api/orders/{id}", id)).andExpect(status().isNoContent());

        mockMvc.perform(get("/api/orders/{id}", id)).andExpect(status().isNotFound());
        assertThat(isDeleted(id)).isTrue();
        assertThat(itemCount(id)).isEqualTo(1);
    }

    @Test
    @DisplayName("DELETE /api/orders/{id} returns 404 when deleting the same order twice")
    void delete_returnsNotFoundWhenDeletedTwice() throws Exception {
        long id = importedOrderId(1);

        mockMvc.perform(delete("/api/orders/{id}", id)).andExpect(status().isNoContent());
        mockMvc.perform(delete("/api/orders/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Order with id " + id + " not found"));
    }

    @Test
    @DisplayName("deleted orders no longer show up in the order search")
    void delete_removesOrderFromSearch() throws Exception {
        mockMvc.perform(delete("/api/orders/{id}", importedOrderId(1))).andExpect(status().isNoContent());

        mockMvc.perform(get("/api/orders").param("to", "2015-01-01"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].orderTime").value("11:57:40"));
    }

    @Test
    @DisplayName("OpenAPI docs point DELETE to the order error responses, whose examples match what it returns")
    void apiDocs_deleteErrorResponsesMatchActualResponses() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(jsonPath("$.paths['/api/orders/{id}'].delete.responses['404']['$ref']")
                        .value("#/components/responses/OrderNotFound"))
                .andExpect(jsonPath("$.paths['/api/orders/{id}'].delete.responses['400']['$ref']")
                        .value("#/components/responses/OrderBadRequest"));

        for (String[] endpoint : List.of(new String[]{"/api/orders/99", "OrderNotFound"},
                new String[]{"/api/orders/abc", "OrderBadRequest"}
        )) {
            String actual = mockMvc.perform(delete(endpoint[0])).andReturn().getResponse().getContentAsString();
            mockMvc.perform(get("/v3/api-docs"))
                    .andExpect(jsonPath("$.components.responses." + endpoint[1] + ".content['application/problem+json'].example")
                            .value(JsonPath.<Object>read(actual, "$")));
        }
    }

    private long importedOrderId(long sourceOrderId) {
        return jdbcClient.sql("SELECT id FROM orders WHERE source_order_id = :id")
                .param("id", sourceOrderId)
                .query(Long.class)
                .single();
    }

    private boolean isDeleted(long orderId) {
        return jdbcClient.sql("SELECT deleted_at IS NOT NULL FROM orders WHERE id = :id")
                .param("id", orderId)
                .query(Boolean.class)
                .single();
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
