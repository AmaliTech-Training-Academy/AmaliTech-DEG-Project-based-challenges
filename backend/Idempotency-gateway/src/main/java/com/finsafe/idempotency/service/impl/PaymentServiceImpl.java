package com.finsafe.idempotency.service.impl;

import com.finsafe.idempotency.dto.PaymentRequestDto;
import com.finsafe.idempotency.dto.PaymentResponseDto;
import com.finsafe.idempotency.enums.RecordStatus;
import com.finsafe.idempotency.exception.ConflictException;
import com.finsafe.idempotency.model.IdempotencyRecord;
import com.finsafe.idempotency.repository.IdempotencyRecordRepository;
import com.finsafe.idempotency.service.PaymentService;
import com.finsafe.idempotency.util.HashUtil;
import com.finsafe.idempotency.util.SerializationUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final IdempotencyRecordRepository repository;
    private final HashUtil hashUtil;

    private final ConcurrentHashMap<String, ReentrantLock> inFlightLocks = new ConcurrentHashMap<>();

    @Override
    public PaymentResponseDto processPayment(String idempotencyKey, PaymentRequestDto request) {
        String incomingRequestHash = hashUtil.hashPayload(request);

        ReentrantLock lock = inFlightLocks.computeIfAbsent(idempotencyKey, k -> new ReentrantLock());
        lock.lock();

        try {
            Optional<IdempotencyRecord> existing = repository.findByIdempotencyKey(idempotencyKey);

            // ── CASE 1: Key exists ───────────────────────────────────────────
            if (existing.isPresent()) {
                IdempotencyRecord record = existing.get();

                // In-flight — lock already forced the wait, re-fetch the result
                if (record.getStatus() == RecordStatus.PROCESSING) {
                    log.info("Key {} was in-flight, re-fetching existing response", idempotencyKey);
                    record = repository.findByIdempotencyKey(idempotencyKey)
                            .orElseThrow(() -> new RuntimeException("Record disappeared unexpectedly"));
                }

                // Completed — validate payload hash
                if (!record.getPayloadHash().equals(incomingRequestHash)) {
                    throw new ConflictException(
                            "Idempotency key already used for a different request body."
                    );
                }

                // Hash matches — return cached response
                log.info("Returning cached response for key: {}", idempotencyKey);
                return SerializationUtil.parse(record.getResponseBody());
            }

            // ── CASE 2: Brand new key ────────────────────────────────────────
            IdempotencyRecord record = IdempotencyRecord.builder()
                    .idempotencyKey(idempotencyKey)
                    .payloadHash(incomingRequestHash)
                    .responseBody("")
                    .statusCode(HttpStatus.CREATED.value())
                    .status(RecordStatus.PROCESSING)
                    .createdAt(LocalDateTime.now())
                    .build();

            repository.save(record);
            log.info("Processing new payment for key: {}", idempotencyKey);

            simulateProcessing();

            PaymentResponseDto response = PaymentResponseDto.builder()
                    .status("SUCCESS")
                    .message("Charged " + request.getAmount()
                            + " " + request.getCurrency())
                    .amount(request.getAmount())
                    .currency(request.getCurrency())
                    .idempotencyKey(idempotencyKey)
                    .build();

            record.setResponseBody(SerializationUtil.stringify(response));
            record.setStatus(RecordStatus.COMPLETED);
            repository.save(record);

            log.info("Payment processed and stored for key: {}", idempotencyKey);
            return response;

        } finally {
            lock.unlock();
            inFlightLocks.remove(idempotencyKey);
        }
    }

    private void simulateProcessing() {
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Payment processing interrupted", e);
        }
    }
}