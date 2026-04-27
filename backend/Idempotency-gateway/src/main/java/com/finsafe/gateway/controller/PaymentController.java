package com.finsafe.gateway.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finsafe.gateway.config.GlobalExceptionHandler;
import com.finsafe.gateway.model.PaymentRequest;
import com.finsafe.gateway.model.PaymentResponse;
import com.finsafe.gateway.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/process-payment")
@Tag(
        name = "Payment Processing",
        description = "Idempotent payment processing endpoint. " +
                "Every request must include a unique Idempotency-Key header."
)
public class PaymentController {

    private static final Logger log = LoggerFactory.getLogger(PaymentController.class);
    private static final String IDEMPOTENCY_HEADER = "Idempotency-Key";
    private static final String CACHE_HIT_HEADER   = "X-Cache-Hit";

    private final PaymentService paymentService;
    private final ObjectMapper   objectMapper;
    private final Validator      validator;

    public PaymentController(PaymentService paymentService,
                             ObjectMapper objectMapper,
                             Validator validator) {
        this.paymentService = paymentService;
        this.objectMapper   = objectMapper;
        this.validator      = validator;
    }

    @Operation(
            summary = "Process a payment",
            description = """
            Processes a payment request with full idempotency protection.
            
            **First request:** Payment is processed (2-second delay simulated) \
            and the response is stored against the key.
            
            **Duplicate request** (same key + same body): The stored response \
            is returned instantly with `X-Cache-Hit: true`. No re-processing.
            
            **Same key + different body:** Rejected with `409 Conflict`.
            
            **Concurrent duplicate:** Waits up to 10 seconds for the original \
            to finish, then replays the result.
            
            **Key expiry:** Keys expire after 24 hours. After expiry the key \
            is treated as new.
            """
    )
    @RequestBody(
            description = "Payment details — amount and currency",
            required = true,
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = PaymentRequest.class),
                    examples = {
                            @ExampleObject(
                                    name = "GHS Payment",
                                    summary = "Pay 100 GHS",
                                    value = """
                                            {
                                              "amount": 100,
                                              "currency": "GHS"
                                            }
                                            """
                            ),
                            @ExampleObject(
                                    name = "USD Payment",
                                    summary = "Pay 250 USD",
                                    value = """
                                            {
                                              "amount": 250,
                                              "currency": "USD"
                                            }
                                            """
                            ),
                            @ExampleObject(
                                    name = "EUR Payment",
                                    summary = "Pay 75 EUR",
                                    value = """
                                            {
                                              "amount": 75,
                                              "currency": "EUR"
                                            }
                                            """
                            )
                    }
            )
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Payment processed successfully — first request",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = PaymentResponse.class),
                            examples = @ExampleObject(
                                    name = "Success",
                                    value = """
                                            {
                                              "status": "Charged 100.0 GHS",
                                              "transactionId": "txn_a3f9c21d88b1"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "201",
                    description = "Cached response — duplicate request replayed",
                    headers = @Header(
                            name = "X-Cache-Hit",
                            description = "true when response is served from cache",
                            schema = @Schema(type = "string", example = "true")
                    ),
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = PaymentResponse.class),
                            examples = @ExampleObject(
                                    name = "Cache hit",
                                    value = """
                                            {
                                              "status": "Charged 100.0 GHS",
                                              "transactionId": "txn_a3f9c21d88b1"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Bad request — missing header or invalid body",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = GlobalExceptionHandler.ErrorResponse.class),
                            examples = {
                                    @ExampleObject(
                                            name = "Missing header",
                                            value = """
                                                    {
                                                      "error": "Idempotency-Key header is required."
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "Validation failed",
                                            value = """
                                                    {
                                                      "error": "Validation failed: amount: must be greater than 0"
                                                    }
                                                    """
                                    )
                            }
                    )
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Conflict — same key reused with different body",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = GlobalExceptionHandler.ErrorResponse.class),
                            examples = @ExampleObject(
                                    name = "Body mismatch",
                                    value = """
                                            {
                                              "error": "Idempotency key already used for a different request body."
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "503",
                    description = "Service unavailable — in-flight request timed out",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = GlobalExceptionHandler.ErrorResponse.class),
                            examples = @ExampleObject(
                                    name = "Timeout",
                                    value = """
                                            {
                                              "error": "Request is still processing after 10000ms. Try again shortly."
                                            }
                                            """
                            )
                    )
            )
    })
    @PostMapping(produces = "application/json")
    public ResponseEntity<?> processPayment(
            @Parameter(
                    name = "Idempotency-Key",
                    description = "A unique string for this logical operation. Use a UUID. " +
                            "Resend the same key to safely retry without double-charging.",
                    required = true,
                    example = "550e8400-e29b-41d4-a716-446655440000",
                    schema = @Schema(type = "string")
            )
            @RequestHeader(value = IDEMPOTENCY_HEADER, required = false) String idempotencyKey,
            HttpServletRequest httpRequest
    ) throws IOException {

        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(new ErrorBody("Idempotency-Key header is required."));
        }

        String rawBody = new String(
                httpRequest.getInputStream().readAllBytes(),
                StandardCharsets.UTF_8
        );

        if (rawBody.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(new ErrorBody("Request body must not be empty."));
        }

        PaymentRequest request;
        try {
            request = objectMapper.readValue(rawBody, PaymentRequest.class);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new ErrorBody("Invalid JSON body: " + e.getMessage()));
        }

        Set<ConstraintViolation<PaymentRequest>> violations = validator.validate(request);
        if (!violations.isEmpty()) {
            String details = violations.stream()
                    .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                    .collect(Collectors.joining("; "));
            return ResponseEntity.badRequest()
                    .body(new ErrorBody("Validation failed: " + details));
        }

        log.info("Received payment request — key={}", idempotencyKey);

        PaymentService.ProcessResult result =
                paymentService.process(idempotencyKey, request, rawBody);

        ResponseEntity.BodyBuilder builder = ResponseEntity.status(result.httpStatus());

        if (result.cacheHit()) {
            builder.header(CACHE_HIT_HEADER, "true");
        }

        return builder.body(result.response());
    }

    private record ErrorBody(String error) {}
}