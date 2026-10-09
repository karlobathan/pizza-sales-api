package com.karlobathan.pizzasales.importer.service;

import com.karlobathan.pizzasales.TestcontainersConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

// same properties as PizzaImportOrchestratorIntegrationTest so both share one cached context and container
@SpringBootTest(properties = {
        "spring.flyway.enabled=true",
        "app.import.pizzas.types-file=classpath:csv/pizza-types.csv",
        "app.import.pizzas.file=classpath:csv/pizzas.csv",
        "app.import.orders.file=classpath:csv/orders.csv",
        "app.import.orders.details-file=classpath:csv/order-details.csv",
        "app.import.chunk-size=2" // 5 orders -> chunks of 2, 2 and 1; 7 order items -> 2, 2, 2 and 1
})
@Import(TestcontainersConfiguration.class)
class OrderImportOrchestratorIntegrationTest {

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
    }

    /** Order items reference pizzas, so every test that imports orders needs the pizza import first. */
    private void importPizzas() {
        pizzaImportOrchestrator.run();
    }

    @Test
    @DisplayName("run persists every order with its source id, date and time")
    void run_persistsOrdersWithSourceIdDateAndTime() {
        importPizzas();
        orderImportOrchestrator.run();

        List<String> orders = jdbcClient.sql("""
                SELECT source_order_id || '|' || order_date || '|' || order_time
                FROM orders
                ORDER BY source_order_id
                """).query(String.class).list();

        assertThat(orders).containsExactly(
                "1|2015-01-01|11:38:36",
                "2|2015-01-01|11:57:40",
                "3|2015-01-02|12:12:28",
                "4|2015-06-15|18:05:00",
                "5|2015-12-31|23:02:05"
        );
    }

    @Test
    @DisplayName("run skips orders whose source id already exists")
    void run_skipsOrdersWhoseSourceIdAlreadyExists() {
        importPizzas();
        jdbcClient.sql("""
                INSERT INTO orders (source_order_id, order_date, order_time)
                VALUES (1, '2020-02-02', '10:00:00')
                """).update();

        orderImportOrchestrator.run();

        assertThat(count("orders")).isEqualTo(5);
        assertThat(jdbcClient.sql("SELECT order_date || '|' || order_time FROM orders WHERE source_order_id = 1")
                .query(String.class)
                .single()).isEqualTo("2020-02-02|10:00:00");
    }

    @Test
    @DisplayName("run persists every order item linked to its order and pizza with its quantity")
    void run_persistsOrderItemsLinkedToOrderAndPizza() {
        importPizzas();
        orderImportOrchestrator.run();

        List<String> orderItems = jdbcClient.sql("""
                SELECT oi.source_order_details_id || '|' || o.source_order_id || '|' || p.code || '|' || oi.quantity
                FROM order_item oi
                JOIN orders o ON o.id = oi.order_id
                JOIN pizza p ON p.id = oi.pizza_id
                ORDER BY oi.source_order_details_id
                """).query(String.class).list();

        assertThat(orderItems).containsExactly(
                "1|1|bbq_ckn_s|1",
                "2|2|pepperoni_xl|1",
                "3|2|ckn_pesto_m|2",
                "4|3|pep_msh_pep_s|1",
                "5|4|bbq_ckn_l|3",
                "6|5|pepperoni_xxl|1",
                "7|5|bbq_ckn_s|1"
        );
    }

    @Test
    @DisplayName("run skips order items whose source id already exists")
    void run_skipsOrderItemsWhoseSourceIdAlreadyExists() {
        importPizzas();
        orderImportOrchestrator.run();
        jdbcClient.sql("UPDATE order_item SET quantity = 9 WHERE source_order_details_id = 1").update();

        orderImportOrchestrator.run();

        assertThat(count("order_item")).isEqualTo(7);
        assertThat(jdbcClient.sql("SELECT quantity FROM order_item WHERE source_order_details_id = 1")
                .query(Integer.class)
                .single()).isEqualTo(9);
    }

    @Test
    @DisplayName("run is idempotent when executed twice")
    void run_isIdempotentWhenExecutedTwice() {
        importPizzas();
        orderImportOrchestrator.run();
        orderImportOrchestrator.run();

        assertThat(count("orders")).isEqualTo(5);
        assertThat(count("order_item")).isEqualTo(7);
    }

    @Test
    @DisplayName("run fails on order items when pizzas have not been imported")
    void run_failsOnOrderItemsWhenPizzasHaveNotBeenImported() {
        assertThatIllegalStateException().isThrownBy(orderImportOrchestrator::run)
                .withMessageContaining("references unknown pizza");
        assertThat(count("orders")).isEqualTo(5);
        assertThat(count("order_item")).isZero();
    }

    private long count(String table) {
        return jdbcClient.sql("SELECT COUNT(*) FROM " + table).query(Long.class).single();
    }
}
