package com.karlobathan.pizzasales.importer.service;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Persists one chunk of entities per call, each call its own transaction.
 * Kept as a separate bean (not a method on the calling service) so Spring's
 * proxy-based {@code @Transactional} actually applies per chunk. Invoking
 * an annotated method on {@code this} from within the same class bypasses
 * the proxy and silently no-ops the transaction boundary.
 */
@Component
@RequiredArgsConstructor
public class ChunkedEntityPersister {

    private final EntityManager entityManager;

    @Transactional
    public <T> void persistChunk(Iterable<T> entities) {
        for (T entity : entities) {
            entityManager.persist(entity);
        }
        entityManager.flush();
        entityManager.clear();
    }
}
