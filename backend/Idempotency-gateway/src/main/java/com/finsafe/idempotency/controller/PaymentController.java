package com.finsafe.idempotency.controller;

import com.finsafe.idempotency.dto.PaymentRequestDto;
import com.finsafe.idempotency.dto.PaymentResponseDto;
import com.finsafe.idempotency.repository.IdempotencyRecordRepository;
import com.finsafe.idempotency.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Payment", description = "Idempotent payment processing API")
public class PaymentController {

    private final PaymentService paymentService;
    private final IdempotencyRecordRepository repository;

    @PostMapping("/process-payment")
    @Operation(
            summary = "Process a payment",
            description = "Processes a payment exactly once using an idempotency key. " +
                    "Duplicate requests return the cached response without reprocessing."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Payment processed successfully"),
            @ApiResponse(responseCode = "200", description = "Duplicate request — cached response returned"),
            @ApiResponse(responseCode = "409", description = "Idempotency key reused with a different payload"),
            @ApiResponse(responseCode = "422", description = "Unprocessable — key conflict detected"),
            @ApiResponse(responseCode = "400", description = "Missing or invalid request body")
    })
    public ResponseEntity<PaymentResponseDto> processPayment(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody PaymentRequestDto request
    ) {
        boolean isNewRequest = repository.findByIdempotencyKey(idempotencyKey).isEmpty();

        PaymentResponseDto response = paymentService.processPayment(idempotencyKey, request);

        if (isNewRequest) {
            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(response);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Cache-Hit", "true");

        return ResponseEntity
                .status(HttpStatus.OK)
                .headers(headers)
                .body(response);
    }
}