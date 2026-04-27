package com.finsafe.gateway.exception;

/**
 * Thrown when a request waits too long for an IN_FLIGHT duplicate to complete.
 * Maps to HTTP 503 Service Unavailable.
 */
public class InFlightTimeoutException extends RuntimeException {
    public InFlightTimeoutException(String message) {
        super(message);
    }
}