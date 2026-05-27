package com.finsafe.idempotency.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finsafe.idempotency.dto.PaymentRequest;
import com.finsafe.idempotency.dto.PaymentResponse;
import com.finsafe.idempotency.exception.BadRequestException;
import com.finsafe.idempotency.exception.IdempotencyConflictException;
import com.finsafe.idempotency.model.PaymentRecord;
import com.finsafe.idempotency.model.PaymentState;
import com.finsafe.idempotency.repository.PaymentRecordRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentService {
    private static final String CONFLICT_MESSAGE = "Idempotency key already used for a different request body.";
    private static final Duration PROCESSING_DELAY = Duration.ofSeconds(2);
    private static final String IDEMPOTENCY_KEY_REGEX = "^[A-Za-z0-9_-]{8,128}$";

    private final PaymentRecordRepository repository;
    private final ObjectMapper objectMapper;
    private final ConcurrentHashMap<String, CompletableFuture<PaymentResult>> inFlightRequests = new ConcurrentHashMap<>();

    public PaymentService(PaymentRecordRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    public PaymentResult processPayment(String idempotencyKey, PaymentRequest request) {
        validateIdempotencyKey(idempotencyKey);
        String requestHash = hashCanonicalPayload(request);
        Optional<PaymentRecord> maybeRecord = repository.findByIdempotencyKey(idempotencyKey);

        if (maybeRecord.isPresent()) {
            return handleExistingRecord(maybeRecord.get(), idempotencyKey, requestHash);
        }

        return createAndProcessRecord(idempotencyKey, requestHash, request);
    }

    private PaymentResult handleExistingRecord(PaymentRecord existingRecord, String idempotencyKey, String requestHash) {
        if (!existingRecord.getRequestHash().equals(requestHash)) {
            throw new IdempotencyConflictException(CONFLICT_MESSAGE);
        }

        if (existingRecord.getState() == PaymentState.COMPLETED) {
            return new PaymentResult(existingRecord.getStatusCode(), existingRecord.getResponseBody(), true);
        }

        CompletableFuture<PaymentResult> future = inFlightRequests.computeIfAbsent(idempotencyKey, key -> new CompletableFuture<>());
        PaymentResult result = future.join();
        return new PaymentResult(result.statusCode(), result.responseBody(), true);
    }

    private PaymentResult createAndProcessRecord(String idempotencyKey, String requestHash, PaymentRequest request) {
        PaymentRecord created = createProcessingRecord(idempotencyKey, requestHash);
        if (!created.getRequestHash().equals(requestHash)) {
            throw new IdempotencyConflictException(CONFLICT_MESSAGE);
        }
        if (created.getState() == PaymentState.COMPLETED) {
            return new PaymentResult(created.getStatusCode(), created.getResponseBody(), true);
        }

        Optional<PaymentRecord> persisted = repository.findByIdempotencyKey(idempotencyKey);
        if (persisted.isPresent() && persisted.get().getState() == PaymentState.PROCESSING && !persisted.get().getId().equals(created.getId())) {
            CompletableFuture<PaymentResult> existingFuture = inFlightRequests.computeIfAbsent(idempotencyKey, key -> new CompletableFuture<>());
            PaymentResult result = existingFuture.join();
            return new PaymentResult(result.statusCode(), result.responseBody(), true);
        }

        CompletableFuture<PaymentResult> future = inFlightRequests.computeIfAbsent(idempotencyKey, key -> new CompletableFuture<>());

        try {
            Thread.sleep(PROCESSING_DELAY.toMillis());

            PaymentResponse response = new PaymentResponse("Charged " + request.getAmount() + " " + request.getCurrency());
            String responseJson = objectMapper.writeValueAsString(response);

            PaymentResult completed = markCompleted(created.getId(), HttpStatus.CREATED.value(), responseJson);
            future.complete(completed);
            return completed;
        } catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
            future.completeExceptionally(interruptedException);
            throw new IllegalStateException("Payment processing interrupted", interruptedException);
        } catch (JsonProcessingException jsonProcessingException) {
            future.completeExceptionally(jsonProcessingException);
            throw new IllegalStateException("Could not serialize payment response", jsonProcessingException);
        } finally {
            inFlightRequests.remove(idempotencyKey, future);
        }
    }

    @Transactional
    protected PaymentRecord createProcessingRecord(String idempotencyKey, String requestHash) {
        try {
            return repository.saveAndFlush(PaymentRecord.processing(idempotencyKey, requestHash));
        } catch (DataIntegrityViolationException duplicateKeyException) {
            PaymentRecord existing = repository.findByIdempotencyKey(idempotencyKey)
                    .orElseThrow(() -> duplicateKeyException);
            return existing;
        }
    }

    @Transactional
    protected PaymentResult markCompleted(Long id, int statusCode, String responseBody) {
        PaymentRecord record = repository.findById(id).orElseThrow();
        record.markCompleted(statusCode, responseBody);
        repository.save(record);
        return new PaymentResult(statusCode, responseBody, false);
    }

    private String hashCanonicalPayload(PaymentRequest request) {
        try {
            JsonNode node = objectMapper.valueToTree(request);
            String canonicalPayload = objectMapper.writeValueAsString(node);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(canonicalPayload.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (JsonProcessingException | NoSuchAlgorithmException exception) {
            throw new IllegalStateException("Could not compute request hash", exception);
        }
    }

    private void validateIdempotencyKey(String idempotencyKey) {
        if (idempotencyKey == null || !idempotencyKey.matches(IDEMPOTENCY_KEY_REGEX)) {
            throw new BadRequestException("Idempotency-Key must be 8-128 chars and contain only letters, numbers, '-' or '_'.");
        }
    }
}
