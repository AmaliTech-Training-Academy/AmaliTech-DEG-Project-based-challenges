package com.finsafe.idempotency.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finsafe.idempotency.dto.PaymentRequest;
import com.finsafe.idempotency.service.PaymentResult;
import com.finsafe.idempotency.service.PaymentService;
import jakarta.validation.Valid;
import java.io.IOException;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
public class PaymentController {
    private final PaymentService paymentService;
    private final ObjectMapper objectMapper;

    public PaymentController(PaymentService paymentService, ObjectMapper objectMapper) {
        this.paymentService = paymentService;
        this.objectMapper = objectMapper;
    }

    @PostMapping("/process-payment")
    public ResponseEntity<JsonNode> processPayment(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody PaymentRequest request
    ) throws IOException {
        PaymentResult result = paymentService.processPayment(idempotencyKey, request);
        JsonNode payload = objectMapper.readTree(result.responseBody());
        return ResponseEntity
                .status(result.statusCode())
                .header("X-Cache-Hit", String.valueOf(result.cacheHit()))
                .body(payload);
    }
}
