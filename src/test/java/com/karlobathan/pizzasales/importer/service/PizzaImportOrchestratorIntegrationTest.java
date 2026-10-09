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
        "app.import.pizza-types-file=classpath:csv/pizza-types.csv"
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
                TRUNCATE pizza_type_ingredient, pizza_type, pizza_ingredient, pizza_category
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
