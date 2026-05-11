package com.finsafe.idempotency.service;

import com.finsafe.idempotency.dto.PaymentRequestDto;
import com.finsafe.idempotency.dto.PaymentResponseDto;

public interface PaymentService {
    PaymentResponseDto processPayment(String idempotencyKey, PaymentRequestDto request);
}