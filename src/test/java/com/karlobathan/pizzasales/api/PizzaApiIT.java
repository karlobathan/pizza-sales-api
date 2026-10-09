package com.karlobathan.pizzasales.api;

import com.jayway.jsonpath.JsonPath;
import com.karlobathan.pizzasales.TestcontainersConfiguration;
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

import java.util.List;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Runs the pizza endpoints end to end against Postgres, with the menu loaded by the real pizza import.
 * Covers what the controller slice tests can't: the repository queries fetch the lazy associations the
 * responses need (open-in-view is off, so a missed fetch fails with a LazyInitializationException).
 */
@SpringBootTest(properties = {
        "spring.flyway.enabled=true",
        "app.import.pizzas.types-file=classpath:csv/pizza-types.csv",
        "app.import.pizzas.file=classpath:csv/pizzas.csv"
})
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class PizzaApiIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PizzaImportOrchestrator pizzaImportOrchestrator;

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
    }

    @Test
    @DisplayName("GET /api/pizza-types returns every imported pizza type with category and ingredients")
    void pizzaTypes_returnsEveryImportedPizzaType() throws Exception {
        // the importer does not assign ids in file order, so compare against the ids actually stored
        List<Integer> idsInOrder = jdbcClient.sql("SELECT id FROM pizza_type ORDER BY id").query(Integer.class).list();

        mockMvc.perform(get("/api/pizza-types"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(4)))
                .andExpect(jsonPath("$[*].id").value(idsInOrder))
                .andExpect(jsonPath("$[*].code", containsInAnyOrder("bbq_ckn", "ckn_pesto", "pep_msh_pep", "pepperoni")))
                .andExpect(jsonPath("$[?(@.code == 'bbq_ckn')].category", contains("Chicken")))
                .andExpect(jsonPath("$[?(@.code == 'bbq_ckn')].ingredients[*]",
                        contains("Barbecue Sauce", "Barbecued Chicken", "Red Peppers", "Tomatoes")
                ));
    }

    @Test
    @DisplayName("GET /api/pizza-types/{id} returns the pizza type with category and ingredients")
    void pizzaType_returnsPizzaTypeWithDetails() throws Exception {
        long id = idOf("pizza_type", "pepperoni");

        mockMvc.perform(get("/api/pizza-types/{id}", id)).andExpect(status().isOk()).andExpect(content().json("""
                {
                  "id": %d,
                  "code": "pepperoni",
                  "name": "The Pepperoni Pizza",
                  "category": "Classic",
                  "ingredients": ["Mozzarella Cheese", "Pepperoni"]
                }
                """.formatted(id), true));
    }

    @Test
    @DisplayName("GET /api/pizza-types/{id} returns 404 for an unknown id")
    void pizzaType_returnsNotFoundForUnknownId() throws Exception {
        mockMvc.perform(get("/api/pizza-types/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Pizza type with id 999999 not found"));
    }

    @Test
    @DisplayName("GET /api/pizzas returns every imported pizza with its pizza type")
    void pizzas_returnsEveryImportedPizza() throws Exception {
        mockMvc.perform(get("/api/pizzas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(6)))
                .andExpect(jsonPath("$[*].code",
                        contains("bbq_ckn_s", "bbq_ckn_l", "ckn_pesto_m", "pep_msh_pep_s", "pepperoni_xl", "pepperoni_xxl")
                ))
                .andExpect(jsonPath("$[0].pizzaType.code").value("bbq_ckn"));
    }

    @Test
    @DisplayName("GET /api/pizzas/{id} returns the pizza with size, price and pizza type")
    void pizza_returnsPizzaWithPizzaType() throws Exception {
        long id = idOf("pizza", "pepperoni_xl");
        long pizzaTypeId = idOf("pizza_type", "pepperoni");

        mockMvc.perform(get("/api/pizzas/{id}", id)).andExpect(status().isOk()).andExpect(content().json("""
                {
                  "id": %d,
                  "code": "pepperoni_xl",
                  "size": "XL",
                  "price": 25.50,
                  "pizzaType": {"id": %d, "code": "pepperoni", "name": "The Pepperoni Pizza"}
                }
                """.formatted(id, pizzaTypeId), true));
    }

    @Test
    @DisplayName("GET /api/pizzas/{id} returns 404 for an unknown id")
    void pizza_returnsNotFoundForUnknownId() throws Exception {
        mockMvc.perform(get("/api/pizzas/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Pizza with id 999999 not found"));
    }

    @Test
    @DisplayName("OpenAPI docs point each by-id endpoint to its own not found and bad request responses")
    void apiDocs_pointByIdEndpointsToTheirOwnErrorResponses() throws Exception {
        for (String[] endpoint : List.of(new String[]{"/api/pizza-types/{id}", "PizzaType"},
                new String[]{"/api/pizzas/{id}", "Pizza"}
        )) {
            String responses = "$.paths['" + endpoint[0] + "'].get.responses";
            mockMvc.perform(get("/v3/api-docs"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath(responses + "['404']['$ref']").value("#/components/responses/" + endpoint[1] + "NotFound"))
                    .andExpect(jsonPath(responses + "['400']['$ref']").value("#/components/responses/" + endpoint[1] + "BadRequest"))
                    .andExpect(jsonPath(responses + "['200'].content['application/json'].schema['$ref']").exists());
        }
        mockMvc.perform(get("/v3/api-docs")).andExpect(jsonPath("$.components.schemas.ProblemDetail").exists());
    }

    @Test
    @DisplayName("OpenAPI error examples match the problem detail each endpoint actually returns")
    void apiDocs_errorExamplesMatchActualResponses() throws Exception {
        for (String[] endpoint : List.of(new String[]{"/api/pizza-types/99", "PizzaTypeNotFound"},
                new String[]{"/api/pizza-types/abc", "PizzaTypeBadRequest"},
                new String[]{"/api/pizzas/99", "PizzaNotFound"},
                new String[]{"/api/pizzas/abc", "PizzaBadRequest"}
        )) {
            String documented = "$.components.responses." + endpoint[1] + ".content['application/problem+json']";
            String actual = mockMvc.perform(get(endpoint[0])).andReturn().getResponse().getContentAsString();

            mockMvc.perform(get("/v3/api-docs"))
                    .andExpect(jsonPath(documented + ".schema['$ref']").value("#/components/schemas/ProblemDetail"))
                    .andExpect(jsonPath(documented + ".example").value(JsonPath.<Object>read(actual, "$")));
        }
    }

    private long idOf(String table, String code) {
        return jdbcClient.sql("SELECT id FROM " + table + " WHERE code = :code")
                .param("code", code)
                .query(Long.class)
                .single();
    }
}
