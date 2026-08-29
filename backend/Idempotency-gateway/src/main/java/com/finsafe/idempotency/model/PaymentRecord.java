package com.finsafe.idempotency.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "payment_records")
public class PaymentRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 255)
    private String idempotencyKey;

    @Column(nullable = false, length = 64)
    private String requestHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentState state;

    @Column
    private Integer statusCode;

    @Column(columnDefinition = "TEXT")
    private String responseBody;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    public static PaymentRecord processing(String idempotencyKey, String requestHash) {
        PaymentRecord record = new PaymentRecord();
        record.idempotencyKey = idempotencyKey;
        record.requestHash = requestHash;
        record.state = PaymentState.PROCESSING;
        Instant now = Instant.now();
        record.createdAt = now;
        record.updatedAt = now;
        return record;
    }

    public void markCompleted(int statusCode, String responseBody) {
        this.state = PaymentState.COMPLETED;
        this.statusCode = statusCode;
        this.responseBody = responseBody;
        this.updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public String getRequestHash() {
        return requestHash;
    }

    public PaymentState getState() {
        return state;
    }

    public Integer getStatusCode() {
        return statusCode;
    }

    public String getResponseBody() {
        return responseBody;
    }
}
