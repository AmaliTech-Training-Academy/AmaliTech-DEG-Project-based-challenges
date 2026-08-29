package com.finsafe.idempotency.service;

public record PaymentResult(
        int statusCode,
        String responseBody,
        boolean cacheHit
) {
}
