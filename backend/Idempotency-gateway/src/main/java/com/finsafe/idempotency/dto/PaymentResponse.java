package com.finsafe.idempotency.dto;

public class PaymentResponse {
    private final String status;

    public PaymentResponse(String status) {
        this.status = status;
    }

    public String getStatus() {
        return status;
    }
}
