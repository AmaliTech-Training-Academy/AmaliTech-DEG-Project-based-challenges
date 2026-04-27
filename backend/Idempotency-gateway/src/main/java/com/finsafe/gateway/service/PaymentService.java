package com.finsafe.gateway.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finsafe.gateway.config.HashUtil;
import com.finsafe.gateway.exception.IdempotencyConflictException;
import com.finsafe.gateway.exception.InFlightTimeoutException;
import com.finsafe.gateway.model.IdempotencyRecord;
import com.finsafe.gateway.model.PaymentRequest;
import com.finsafe.gateway.model.PaymentResponse;
import com.finsafe.gateway.repository.IdempotencyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

/**
 * Core service implementing the idempotency gateway logic.
 *
 * <h3>Flow summary</h3>
 * <pre>
 * 1. Hash the incoming request body (SHA-256).
 * 2. Look up the key in SQLite.
 *    a. Not found / expired  → insert IN_FLIGHT, process, mark DONE, return 201.
 *    b. Found, IN_FLIGHT     → poll until DONE (race-condition protection).
 *    c. Found, DONE, same hash → return cached response with X-Cache-Hit: true.
 *    d. Found, DONE, diff hash → throw 409 Conflict (fraud/error check).
 * </pre>
 *
 * <h3>Developer's Choice — TTL expiry</h3>
 * Every record carries an {@code expires_at} timestamp computed from
 * {@code idempotency.ttl-minutes}.  Expired records are treated as absent,
 * allowing legitimate key reuse after the window closes.  A {@link Scheduled}
 * task purges expired rows nightly so the SQLite file stays lean.
 */
@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private final IdempotencyRepository repo;
    private final ObjectMapper           objectMapper;

    @Value("${idempotency.ttl-minutes:1440}")
    private long ttlMinutes;

    @Value("${idempotency.poll-interval-ms:200}")
    private long pollIntervalMs;

    @Value("${idempotency.max-wait-ms:10000}")
    private long maxWaitMs;

    public PaymentService(IdempotencyRepository repo, ObjectMapper objectMapper) {
        this.repo         = repo;
        this.objectMapper = objectMapper;
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Result wrapper returned to the controller.
     * Carries the response body, the HTTP status code to use, and whether
     * this response was served from the idempotency cache.
     */
    public record ProcessResult(PaymentResponse response, int httpStatus, boolean cacheHit) {}

    /**
     * Process (or replay) a payment request.
     *
     * @param idempotencyKey the value of the Idempotency-Key header
     * @param request        the validated request body
     * @param rawBody        the raw JSON string (used for hashing)
     * @return a {@link ProcessResult} describing the outcome
     */
    public ProcessResult process(String idempotencyKey, PaymentRequest request, String rawBody) {

        String incomingHash = HashUtil.sha256Hex(rawBody);

        // Synchronize on the key to prevent race conditions within same JVM instance
        synchronized (("idempotency-" + idempotencyKey).intern()) {

            Optional<IdempotencyRecord> existing = repo.findByKey(idempotencyKey);

            // Developer's Choice: treat expired records as absent
            if (existing.isPresent() && existing.get().isExpired()) {
                log.info("Key '{}' expired — deleting and treating as new", idempotencyKey);
                repo.delete(idempotencyKey);
                existing = Optional.empty();
            }

            if (existing.isEmpty()) {
                return processNewRequest(idempotencyKey, request, incomingHash);
            }

            IdempotencyRecord record = existing.get();

            return switch (record.getStatus()) {
                case IN_FLIGHT -> waitForInFlight(idempotencyKey, incomingHash);
                case DONE      -> replayOrConflict(record, incomingHash);
            };
        }
    }
    // ── Private helpers ───────────────────────────────────────────────────────

    /**
     * Insert an IN_FLIGHT sentinel, run the simulated payment, then update to DONE.
     * If processing throws, the IN_FLIGHT row is deleted so the client can retry.
     */
    private ProcessResult processNewRequest(String key, PaymentRequest request, String bodyHash) {
        Instant now       = Instant.now();
        Instant expiresAt = now.plus(ttlMinutes, ChronoUnit.MINUTES);

        repo.insertInFlight(key, bodyHash, now, expiresAt);
        log.debug("Key '{}' inserted as IN_FLIGHT", key);

        try {
            PaymentResponse response = simulatePaymentProcessing(request);
            String responseJson = serialize(response);
            repo.markDone(key, responseJson, 201);
            log.info("Key '{}' processing complete → DONE", key);
            return new ProcessResult(response, 201, false);

        } catch (Exception e) {
            // Processing failed — remove the sentinel so the client can retry safely.
            log.error("Processing failed for key '{}', removing IN_FLIGHT record", key, e);
            repo.delete(key);
            throw new RuntimeException("Payment processing failed: " + e.getMessage(), e);
        }
    }

    /**
     * Bonus: race-condition protection.
     * Block the current thread by polling SQLite until the original request
     * finishes (status transitions from IN_FLIGHT → DONE) or until maxWaitMs.
     */
    private ProcessResult waitForInFlight(String key, String incomingHash) {
        log.info("Key '{}' is IN_FLIGHT — waiting for original request to finish", key);
        long deadline = System.currentTimeMillis() + maxWaitMs;

        while (System.currentTimeMillis() < deadline) {
            try {
                Thread.sleep(pollIntervalMs);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Interrupted while waiting for in-flight request", ie);
            }

            Optional<IdempotencyRecord> updated = repo.findByKey(key);

            if (updated.isEmpty()) {
                // Original request failed and cleaned up — let this one through as new.
                log.warn("IN_FLIGHT record for key '{}' disappeared (original failed) — treating as new", key);
                // Re-look up with a fresh hash since we already have it
                throw new RuntimeException("Original request failed. Please retry.");
            }

            IdempotencyRecord record = updated.get();
            if (record.getStatus() == IdempotencyRecord.Status.DONE) {
                log.info("Key '{}' transitioned to DONE while waiting — replaying result", key);
                return replayOrConflict(record, incomingHash);
            }
        }

        throw new InFlightTimeoutException(
                "Request with key '" + key + "' is still processing after " + maxWaitMs + "ms. Try again shortly.");
    }

    /**
     * The key exists and is DONE.
     * If the body hash matches → replay cached response.
     * If not → reject as a conflicting request.
     */
    private ProcessResult replayOrConflict(IdempotencyRecord record, String incomingHash) {
        if (!record.getBodyHash().equals(incomingHash)) {
            log.warn("Key '{}' reused with a different request body — rejecting", record.getIdempotencyKey());
            throw new IdempotencyConflictException(
                    "Idempotency key already used for a different request body.");
        }

        log.info("Key '{}' cache hit — replaying stored response", record.getIdempotencyKey());
        PaymentResponse response = deserialize(record.getResponseBody());
        return new ProcessResult(response, record.getResponseStatus(), true);
    }

    /**
     * Simulate the 2-second payment processing delay required by the spec.
     */
    private PaymentResponse simulatePaymentProcessing(PaymentRequest request) {
        log.debug("Simulating payment processing (2s delay)...");
        try {
            Thread.sleep(2_000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Processing interrupted", e);
        }
        String txnId  = "txn_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        String status = String.format("Charged %.1f %s", request.getAmount(), request.getCurrency());
        return new PaymentResponse(status, txnId);
    }

    // ── JSON helpers ──────────────────────────────────────────────────────────

    private String serialize(PaymentResponse response) {
        try {
            return objectMapper.writeValueAsString(response);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialise response", e);
        }
    }

    private PaymentResponse deserialize(String json) {
        try {
            return objectMapper.readValue(json, PaymentResponse.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to deserialise cached response", e);
        }
    }

    // ── Developer's Choice: scheduled cleanup ─────────────────────────────────

    /**
     * Purge expired idempotency records every day at 02:00.
     * Keeps the SQLite database file from growing unbounded.
     */
    @Scheduled(cron = "0 0 2 * * *")
    public void purgeExpiredRecords() {
        int deleted = repo.deleteExpired();
        if (deleted > 0) {
            log.info("Scheduled cleanup: deleted {} expired idempotency records", deleted);
        }
    }
}