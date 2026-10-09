package com.karlobathan.pizzasales.api.exception;

import java.time.LocalDate;

/**
 * Thrown when a request is well-formed but its values don't make sense together; rendered as a 400 by
 * {@link ApiExceptionHandler}.
 */
public class InvalidRequestException extends RuntimeException {

    public InvalidRequestException(String message) {
        super(message);
    }

    /**
     * For a date range whose start is after its end. The OpenAPI example uses this too, so docs and API agree.
     */
    public static InvalidRequestException fromAfterTo(LocalDate from, LocalDate to) {
        return new InvalidRequestException("'from' (%s) must not be after 'to' (%s)".formatted(from, to));
    }
}
