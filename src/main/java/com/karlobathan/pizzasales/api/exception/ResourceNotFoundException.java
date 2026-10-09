package com.karlobathan.pizzasales.api.exception;

/**
 * Thrown when a resource looked up by id does not exist; rendered as a 404 by {@link ApiExceptionHandler}.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String resource, Object id) {
        super("%s with id %s not found".formatted(resource, id));
    }
}
