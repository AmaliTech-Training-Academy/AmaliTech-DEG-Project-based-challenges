package com.finsafe.gateway.exception;

/**
 * Thrown when a request reuses an idempotency key with a different request body.
 * Maps to HTTP 409 Conflict.
 */
public class IdempotencyConflictException extends RuntimeException {
    public IdempotencyConflictException(String message) {
        super(message);
    }
}