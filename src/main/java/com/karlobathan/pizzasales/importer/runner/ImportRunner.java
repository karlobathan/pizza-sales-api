package com.karlobathan.pizzasales.importer.runner;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * ApplicationRunner implementation that runs the import orchestrator and exits the application
 * with an appropriate exit code.
 */
@RequiredArgsConstructor
@Slf4j
public class ImportRunner implements ApplicationRunner {

    private final Runnable orchestrator;
    private final ConfigurableApplicationContext context;

    @Override
    public void run(ApplicationArguments args) {
        int exitCode = runImport();
        System.exit(SpringApplication.exit(context, () -> exitCode));
    }

    private int runImport() {
        try {
            orchestrator.run();
            return 0;
        } catch (Exception e) {
            log.error("Import failed", e);
            return 1;
        }
    }
}
