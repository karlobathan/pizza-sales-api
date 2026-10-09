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
 * Runs the pizza type write endpoints end to end against Postgres, on top of the menu and orders loaded by the real
 * imports: categories and ingredients matched by name or created, codes reserved even after a delete, deleting only a
 * pizza type without pizzas, soft-deleted pizza types hidden but not recreated by a re-import, and docs examples.
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
class PizzaTypeWriteApiIT {

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
    @DisplayName("POST /api/pizza-types reuses the category and ingredients matched ignoring case and creates new ones")
    void create_reusesAndCreatesCategoryAndIngredients() throws Exception {
        MvcResult result = createPizzaType("""
                {"code": "basil_pep", "name": "The Basil Pepperoni Pizza", "category": "classic",
                 "ingredients": ["PEPPERONI", "Basil", "basil"]}
                """)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("basil_pep"))
                .andExpect(jsonPath("$.category").value("Classic"))
                .andExpect(jsonPath("$.ingredients", contains("Basil", "Pepperoni")))
                .andReturn();

        mockMvc.perform(get(result.getResponse().getHeader("Location")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(idFrom(result)));
        assertThat(count("pizza_category")).isEqualTo(2);
        assertThat(count("pizza_ingredient")).isEqualTo(10);
        mockMvc.perform(get("/api/pizza-types")).andExpect(jsonPath("$[*].code", hasItem("basil_pep")));
    }

    @Test
    @DisplayName("POST /api/pizza-types creates a category that doesn't exist yet")
    void create_createsNewCategory() throws Exception {
        createPizzaType("""
                {"code": "nutella", "name": "The Nutella Pizza", "category": "Dessert", "ingredients": ["Nutella"]}
                """)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.category").value("Dessert"));

        assertThat(count("pizza_category")).isEqualTo(3);
    }

    @Test
    @DisplayName("POST /api/pizza-types returns 409 for a code that is used, even by a deleted pizza type")
    void create_returnsConflictForUsedCode() throws Exception {
        createPizzaType(pizzaTypeJson("pepperoni")).andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Pizza type code 'pepperoni' is already in use"));

        long id = idFrom(createPizzaType(pizzaTypeJson("short_lived")).andExpect(status().isCreated()).andReturn());
        mockMvc.perform(delete("/api/pizza-types/{id}", id)).andExpect(status().isNoContent());
        createPizzaType(pizzaTypeJson("short_lived")).andExpect(status().isConflict());
        assertThat(count("pizza_type")).isEqualTo(5);
    }

    @Test
    @DisplayName("PUT /api/pizza-types/{id} replaces name, category and ingredients and keeps the code")
    void replace_replacesDetailsAndKeepsCode() throws Exception {
        long id = idOf("pizza_type", "pepperoni");

        mockMvc.perform(put("/api/pizza-types/{id}", id).contentType(MediaType.APPLICATION_JSON).content("""
                        {"code": "ignored", "name": "The Spicy Pepperoni Pizza", "category": "Spicy",
                         "ingredients": ["Pepperoni", "Chilli Flakes"]}
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("pepperoni"))
                .andExpect(jsonPath("$.name").value("The Spicy Pepperoni Pizza"))
                .andExpect(jsonPath("$.category").value("Spicy"))
                .andExpect(jsonPath("$.ingredients", contains("Chilli Flakes", "Pepperoni")));

        mockMvc.perform(get("/api/pizza-types/{id}", id)).andExpect(jsonPath("$.ingredients", contains("Chilli Flakes", "Pepperoni")));
        mockMvc.perform(get("/api/pizzas/{id}", idOf("pizza", "pepperoni_xl")))
                .andExpect(jsonPath("$.pizzaType.name").value("The Spicy Pepperoni Pizza"));
    }

    @Test
    @DisplayName("PUT /api/pizza-types/{id} returns 404 for an unknown or deleted pizza type")
    void replace_returnsNotFoundForUnknownOrDeleted() throws Exception {
        long deleted = idFrom(createPizzaType(pizzaTypeJson("short_lived")).andReturn());
        mockMvc.perform(delete("/api/pizza-types/{id}", deleted)).andExpect(status().isNoContent());

        for (long id : List.of(999999L, deleted)) {
            mockMvc.perform(put("/api/pizza-types/{id}", id).contentType(MediaType.APPLICATION_JSON).content("""
                            {"name": "Renamed", "category": "Classic", "ingredients": ["Pepperoni"]}
                            """))
                    .andExpect(status().isNotFound());
        }
    }

    @Test
    @DisplayName("DELETE /api/pizza-types/{id} returns 409 while the pizza type has pizzas and leaves it on the menu")
    void delete_returnsConflictWhilePizzaTypeHasPizzas() throws Exception {
        long id = idOf("pizza_type", "pepperoni");

        mockMvc.perform(delete("/api/pizza-types/{id}", id))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Pizza type with id " + id + " still has pizzas; delete them first"));

        mockMvc.perform(get("/api/pizza-types/{id}", id)).andExpect(status().isOk());
        assertThat(isDeleted("pizza_type", "pepperoni")).isFalse();
    }

    @Test
    @DisplayName("DELETE /api/pizza-types/{id} hides a pizza type whose pizzas are deleted, and a re-import doesn't bring it back")
    void delete_hidesPizzaTypeAndSurvivesReimport() throws Exception {
        long id = idOf("pizza_type", "pepperoni");
        mockMvc.perform(delete("/api/pizzas/{id}", idOf("pizza", "pepperoni_xl"))).andExpect(status().isNoContent());
        mockMvc.perform(delete("/api/pizzas/{id}", idOf("pizza", "pepperoni_xxl"))).andExpect(status().isNoContent());

        mockMvc.perform(delete("/api/pizza-types/{id}", id)).andExpect(status().isNoContent());

        mockMvc.perform(get("/api/pizza-types/{id}", id)).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/pizza-types")).andExpect(jsonPath("$[*].code", not(hasItem("pepperoni"))));
        mockMvc.perform(delete("/api/pizza-types/{id}", id)).andExpect(status().isNotFound());
        assertThat(isDeleted("pizza_type", "pepperoni")).isTrue();

        pizzaImportOrchestrator.run();

        assertThat(count("pizza_type")).isEqualTo(4);
        assertThat(count("pizza")).isEqualTo(6);
        assertThat(isDeleted("pizza_type", "pepperoni")).isTrue();
        mockMvc.perform(get("/api/pizza-types/{id}", id)).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("OpenAPI docs point the pizza type writes to their error responses, whose examples match what they return")
    void apiDocs_pizzaTypeWriteErrorsMatchActualResponses() throws Exception {
        String paths = "$.paths['/api/pizza-types']";
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(jsonPath(paths + ".post.responses['400']['$ref']").value("#/components/responses/PizzaTypeWriteBadRequest"))
                .andExpect(jsonPath(paths + ".post.responses['409']['$ref']").value("#/components/responses/PizzaTypeConflict"))
                .andExpect(jsonPath("$.paths['/api/pizza-types/{id}'].put.responses['404']['$ref']")
                        .value("#/components/responses/PizzaTypeNotFound"))
                .andExpect(jsonPath("$.paths['/api/pizza-types/{id}'].delete.responses['409']['$ref']")
                        .value("#/components/responses/PizzaTypeConflict"));

        String badRequest = createPizzaType("""
                {"code": "Not Valid", "name": "The Pizza", "category": "Classic", "ingredients": ["Pepperoni"]}
                """).andReturn().getResponse().getContentAsString();
        String conflict = createPizzaType(pizzaTypeJson("pepperoni")).andReturn().getResponse().getContentAsString();
        String examples = "$.components.responses.%s.content['application/problem+json'].example";
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(jsonPath(examples.formatted("PizzaTypeWriteBadRequest")).value(json(badRequest)))
                .andExpect(jsonPath(examples.formatted("PizzaTypeConflict")).value(json(conflict)));
    }

    private ResultActions createPizzaType(String json) throws Exception {
        return mockMvc.perform(post("/api/pizza-types").contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private static String pizzaTypeJson(String code) {
        return """
                {"code": "%s", "name": "The Pizza", "category": "Classic", "ingredients": ["Pepperoni"]}
                """.formatted(code);
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

