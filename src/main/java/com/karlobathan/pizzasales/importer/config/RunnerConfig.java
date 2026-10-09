package com.karlobathan.pizzasales.importer.config;

import com.karlobathan.pizzasales.importer.runner.ImportRunner;
import com.karlobathan.pizzasales.importer.service.PizzaImportOrchestrator;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@EnableConfigurationProperties(ImportProperties.class)
public class RunnerConfig {

    @Profile("import-pizzas")
    @Bean
    public ApplicationRunner pizzaImportRunner(PizzaImportOrchestrator orchestrator, ConfigurableApplicationContext context) {
        return new ImportRunner(orchestrator, context);
    }
}