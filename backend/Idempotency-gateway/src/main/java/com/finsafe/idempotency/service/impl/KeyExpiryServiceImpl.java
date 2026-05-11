package com.finsafe.idempotency.service.impl;

import com.finsafe.idempotency.repository.IdempotencyRecordRepository;
import com.finsafe.idempotency.service.KeyExpiryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

import com.finsafe.idempotency.model.IdempotencyRecord;

@Slf4j
@Service
@RequiredArgsConstructor
public class KeyExpiryServiceImpl implements KeyExpiryService {

    private final IdempotencyRecordRepository repository;

    @Value("${app.idempotency.ttl-hours}")
    private int ttlHours;

    // Runs every hour to check for expired keys
    @Scheduled(fixedRate = 3600000)
    @Override
    public void cleanupExpiredKeys() {
        // Calculate time limit: All records older than 36 hours are expired
        LocalDateTime timeLimit = LocalDateTime.now().minusHours(ttlHours);

        List<IdempotencyRecord> expiredRecords = repository.findAllByCreatedAtBefore(timeLimit);

        if (expiredRecords.isEmpty()) {
            log.info("Key expiry cleanup ran — no expired keys found");
            return;
        }

        repository.deleteAll(expiredRecords);

        log.info("Key expiry cleanup ran — deleted {} expired idempotency key(s)", expiredRecords.size());
    }
}