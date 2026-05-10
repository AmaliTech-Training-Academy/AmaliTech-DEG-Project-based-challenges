package com.finsafe.idempotency.repository;

import com.finsafe.idempotency.model.IdempotencyRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface IdempotencyRecordRepository extends JpaRepository<IdempotencyRecord, Long> {

    Optional<IdempotencyRecord> findByIdempotencyKey(String idempotencyKey);

    List<IdempotencyRecord> findAllByCreatedAtBefore(LocalDateTime limit);
}