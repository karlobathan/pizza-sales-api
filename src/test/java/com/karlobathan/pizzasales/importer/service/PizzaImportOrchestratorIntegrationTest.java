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

@SpringBootTest(properties = {
        "spring.flyway.enabled=true",
        "app.import.pizza-types-file=classpath:csv/pizza-types.csv",
        "app.import.pizzas-file=classpath:csv/pizzas.csv",
        "app.import.orders-file=classpath:csv/orders.csv",
        "app.import.chunk-size=2" // 5 orders -> chunks of 2, 2 and 1
})
@Import(TestcontainersConfiguration.class)
class PizzaImportOrchestratorIntegrationTest {

    @Autowired
    private PizzaImportOrchestrator orchestrator;

    @Autowired
    private JdbcClient jdbcClient;

    @BeforeEach
    void setUp() {
        jdbcClient.sql("""
                TRUNCATE orders, pizza, pizza_type_ingredient, pizza_type, pizza_ingredient, pizza_category
                RESTART IDENTITY CASCADE
                """).update();
    }

    @Test
    @DisplayName("run persists every pizza type with its name and category")
    void run_persistsPizzaTypesWithNameAndCategory() {
        orchestrator.run();

        List<String> pizzaTypes = jdbcClient.sql("""
                SELECT pt.code || '|' || pt.name || '|' || pc.name
                FROM pizza_type pt
                JOIN pizza_category pc ON pc.id = pt.pizza_category_id
                ORDER BY pt.code
                """).query(String.class).list();

        assertThat(pizzaTypes).containsExactly(
                "bbq_ckn|The Barbecue Chicken Pizza|Chicken",
                "ckn_pesto|The Chicken Pesto Pizza|Chicken",
                "pep_msh_pep|The Pepperoni, Mushroom, and Peppers Pizza|Classic",
                "pepperoni|The Pepperoni Pizza|Classic"
        );
    }

    @Test
    @DisplayName("run links each pizza type to its ingredients")
    void run_linksPizzaTypesToIngredients() {
        orchestrator.run();

        assertThat(ingredientsOf("bbq_ckn")).containsExactlyInAnyOrder("Barbecued Chicken",
                "Red Peppers",
                "Tomatoes",
                "Barbecue Sauce"
        );
        assertThat(ingredientsOf("ckn_pesto")).containsExactlyInAnyOrder("Chicken",
                "Tomatoes",
                "Red Peppers",
                "Pesto Sauce"
        );
        assertThat(ingredientsOf("pep_msh_pep")).containsExactlyInAnyOrder("Pepperoni", "Mushrooms", "Red Peppers");
        assertThat(ingredientsOf("pepperoni")).containsExactlyInAnyOrder("Mozzarella Cheese", "Pepperoni");
    }

    @Test
    @DisplayName("run deduplicates categories and ingredients case-insensitively keeping the first spelling")
    void run_deduplicatesCategoriesAndIngredientsCaseInsensitively() {
        orchestrator.run();

        assertThat(categoryNames()).containsExactly("Chicken", "Classic");
        assertThat(ingredientNames()).containsExactly("Barbecue Sauce",
                "Barbecued Chicken",
                "Chicken",
                "Mozzarella Cheese",
                "Mushrooms",
                "Pepperoni",
                "Pesto Sauce",
                "Red Peppers",
                "Tomatoes"
        );
    }

    @Test
    @DisplayName("run is idempotent when executed twice")
    void run_isIdempotentWhenExecutedTwice() {
        orchestrator.run();
        orchestrator.run();

        assertThat(count("pizza_type")).isEqualTo(4);
        assertThat(count("pizza_category")).isEqualTo(2);
        assertThat(count("pizza_ingredient")).isEqualTo(9);
        assertThat(count("pizza_type_ingredient")).isEqualTo(13);
        assertThat(count("pizza")).isEqualTo(6);
        assertThat(count("orders")).isEqualTo(5);
    }

    @Test
    @DisplayName("run reuses existing categories and ingredients")
    void run_reusesExistingCategoriesAndIngredients() {
        long categoryId = jdbcClient.sql("INSERT INTO pizza_category (name) VALUES ('classic') RETURNING id")
                .query(Long.class)
                .single();
        long ingredientId = jdbcClient.sql("INSERT INTO pizza_ingredient (name) VALUES ('pepperoni') RETURNING id")
                .query(Long.class)
                .single();

        orchestrator.run();

        assertThat(categoryNames()).containsExactly("Chicken", "classic");
        assertThat(ingredientNames()).contains("pepperoni").doesNotContain("Pepperoni");
        assertThat(jdbcClient.sql("SELECT pizza_category_id FROM pizza_type WHERE code = 'pepperoni'")
                .query(Long.class)
                .single()).isEqualTo(categoryId);
        assertThat(jdbcClient.sql("""
                SELECT COUNT(*)
                FROM pizza_type_ingredient
                WHERE pizza_ingredient_id = :ingredientId
                """).param("ingredientId", ingredientId).query(Long.class).single()).isEqualTo(2);
    }

    @Test
    @DisplayName("run skips pizza types whose code already exists")
    void run_skipsPizzaTypesWhoseCodeAlreadyExists() {
        long categoryId = jdbcClient.sql("INSERT INTO pizza_category (name) VALUES ('Classic') RETURNING id")
                .query(Long.class)
                .single();
        jdbcClient.sql("INSERT INTO pizza_type (code, name, pizza_category_id) VALUES ('pepperoni', 'Original', :id)")
                .param("id", categoryId)
                .update();

        orchestrator.run();

        assertThat(count("pizza_type")).isEqualTo(4);
        assertThat(jdbcClient.sql("SELECT name FROM pizza_type WHERE code = 'pepperoni'")
                .query(String.class)
                .single()).isEqualTo("Original");
        assertThat(ingredientsOf("pepperoni")).isEmpty();
        assertThat(ingredientNames()).doesNotContain("Mozzarella Cheese");
    }

    @Test
    @DisplayName("run persists every pizza with its pizza type, size and price")
    void run_persistsPizzasWithPizzaTypeSizeAndPrice() {
        orchestrator.run();

        List<String> pizzas = jdbcClient.sql("""
                SELECT p.code || '|' || pt.code || '|' || p.size || '|' || p.price
                FROM pizza p
                JOIN pizza_type pt ON pt.id = p.pizza_type_id
                ORDER BY p.code
                """).query(String.class).list();

        assertThat(pizzas).containsExactly(
                "bbq_ckn_l|bbq_ckn|L|20.75",
                "bbq_ckn_s|bbq_ckn|S|12.75",
                "ckn_pesto_m|ckn_pesto|M|16.75",
                "pep_msh_pep_s|pep_msh_pep|S|11.00",
                "pepperoni_xl|pepperoni|XL|25.50",
                "pepperoni_xxl|pepperoni|XXL|35.95"
        );
    }

    @Test
    @DisplayName("run skips pizzas whose code already exists")
    void run_skipsPizzasWhoseCodeAlreadyExists() {
        long categoryId = jdbcClient.sql("INSERT INTO pizza_category (name) VALUES ('Classic') RETURNING id")
                .query(Long.class)
                .single();
        long pizzaTypeId = jdbcClient.sql("""
                INSERT INTO pizza_type (code, name, pizza_category_id)
                VALUES ('pepperoni', 'The Pepperoni Pizza', :id)
                RETURNING id
                """).param("id", categoryId).query(Long.class).single();
        jdbcClient.sql("INSERT INTO pizza (code, pizza_type_id, size, price) VALUES ('pepperoni_xl', :id, 'XL', 99.99)")
                .param("id", pizzaTypeId)
                .update();

        orchestrator.run();

        assertThat(count("pizza")).isEqualTo(6);
        assertThat(jdbcClient.sql("SELECT price FROM pizza WHERE code = 'pepperoni_xl'")
                .query(String.class)
                .single()).isEqualTo("99.99");
        assertThat(jdbcClient.sql("""
                SELECT COUNT(*)
                FROM pizza p
                JOIN pizza_type pt ON pt.id = p.pizza_type_id
                WHERE pt.code = 'pepperoni'
                """).query(Long.class).single()).isEqualTo(2);
    }

    @Test
    @DisplayName("run persists every order with its source id, date and time")
    void run_persistsOrdersWithSourceIdDateAndTime() {
        orchestrator.run();

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
        jdbcClient.sql("""
                INSERT INTO orders (source_order_id, order_date, order_time)
                VALUES (1, '2020-02-02', '10:00:00')
                """).update();

        orchestrator.run();

        assertThat(count("orders")).isEqualTo(5);
        assertThat(jdbcClient.sql("SELECT order_date || '|' || order_time FROM orders WHERE source_order_id = 1")
                .query(String.class)
                .single()).isEqualTo("2020-02-02|10:00:00");
    }

    private List<String> ingredientsOf(String pizzaTypeCode) {
        return jdbcClient.sql("""
                SELECT pi.name
                FROM pizza_type_ingredient pti
                JOIN pizza_type pt ON pt.id = pti.pizza_type_id
                JOIN pizza_ingredient pi ON pi.id = pti.pizza_ingredient_id
                WHERE pt.code = :code
                """).param("code", pizzaTypeCode).query(String.class).list();
    }

    private List<String> categoryNames() {
        return jdbcClient.sql("SELECT name FROM pizza_category ORDER BY name COLLATE \"C\"").query(String.class).list();
    }

    private List<String> ingredientNames() {
        return jdbcClient.sql("SELECT name FROM pizza_ingredient ORDER BY name COLLATE \"C\"").query(String.class).list();
    }

    private long count(String table) {
        return jdbcClient.sql("SELECT COUNT(*) FROM " + table).query(Long.class).single();
    }
}
