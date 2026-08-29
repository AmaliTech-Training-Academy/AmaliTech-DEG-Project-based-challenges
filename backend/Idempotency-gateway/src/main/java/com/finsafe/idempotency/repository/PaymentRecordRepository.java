package com.finsafe.idempotency.repository;

import com.finsafe.idempotency.model.PaymentRecord;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRecordRepository extends JpaRepository<PaymentRecord, Long> {
    Optional<PaymentRecord> findByIdempotencyKey(String idempotencyKey);
}
