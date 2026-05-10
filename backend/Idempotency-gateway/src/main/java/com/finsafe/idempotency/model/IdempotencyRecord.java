package com.finsafe.idempotency.model;

import com.finsafe.idempotency.enums.RecordStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "idempotency_records")
public class IdempotencyRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "idempotency_key", nullable = false, unique = true)
    private String idempotencyKey;

    @Column(name = "payload_hash", nullable = false)
    private String payloadHash;

    @Column(name = "response_body", nullable = false)
    private String responseBody;

    @Column(name = "status_code", nullable = false)
    private int statusCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RecordStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}