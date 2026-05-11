package com.finsafe.idempotency.service;

public interface KeyExpiryService {
    void cleanupExpiredKeys();
}