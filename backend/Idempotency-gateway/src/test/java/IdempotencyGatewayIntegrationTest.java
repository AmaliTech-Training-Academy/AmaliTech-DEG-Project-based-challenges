package com.finsafe.gateway;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finsafe.gateway.model.PaymentResponse;
import com.finsafe.gateway.repository.IdempotencyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for the Idempotency Gateway.
 * Uses an in-memory SQLite database (file::memory:?cache=shared) per test run.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:sqlite:file::memory:?cache=shared",
        "idempotency.ttl-minutes=1440",
        "idempotency.poll-interval-ms=100",
        "idempotency.max-wait-ms=8000"
})
class IdempotencyGatewayIntegrationTest {

    private static final String ENDPOINT = "/process-payment";
    private static final String KEY_HDR  = "Idempotency-Key";

    @Autowired MockMvc         mvc;
    @Autowired ObjectMapper    mapper;
    @Autowired IdempotencyRepository repo;

    @BeforeEach
    void cleanDb() {
        repo.deleteExpired(); // just to exercise the method; DB is reset per test run via in-memory
    }

    // ── User Story 1: Happy Path ──────────────────────────────────────────────

    @Test
    @DisplayName("US1 — first request is processed and returns 201 Created")
    void firstRequestReturns201() throws Exception {
        String key  = uniqueKey();
        String body = """
                {"amount": 100, "currency": "GHS"}""";

        mvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(KEY_HDR, key)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("Charged 100.0 GHS"))
                .andExpect(jsonPath("$.transactionId").isNotEmpty());
    }

    @Test
    @DisplayName("US1 — missing Idempotency-Key header returns 400")
    void missingHeaderReturns400() throws Exception {
        mvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"amount": 50, "currency": "USD"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").isNotEmpty());
    }

    // ── User Story 2: Duplicate request (idempotency replay) ─────────────────

    @Test
    @DisplayName("US2 — duplicate request returns same body, 201, and X-Cache-Hit: true")
    void duplicateRequestReplaysCachedResponse() throws Exception {
        String key  = uniqueKey();
        String body = """
                {"amount": 200, "currency": "USD"}""";

        // First call
        MvcResult first = mvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(KEY_HDR, key)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();

        PaymentResponse r1 = mapper.readValue(first.getResponse().getContentAsString(), PaymentResponse.class);

        // Second call — same key, same body
        MvcResult second = mvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(KEY_HDR, key)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(header().string("X-Cache-Hit", "true"))
                .andReturn();

        PaymentResponse r2 = mapper.readValue(second.getResponse().getContentAsString(), PaymentResponse.class);

        assertThat(r1.getTransactionId()).isEqualTo(r2.getTransactionId());
        assertThat(r1.getStatus()).isEqualTo(r2.getStatus());
    }

    @Test
    @DisplayName("US2 — duplicate request does NOT add another 2-second delay")
    void duplicateRequestIsInstant() throws Exception {
        String key  = uniqueKey();
        String body = """
                {"amount": 75, "currency": "EUR"}""";

        // First call (will take ~2s)
        mvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(KEY_HDR, key)
                        .content(body))
                .andExpect(status().isCreated());

        // Second call should complete well under 1 second
        long start = System.currentTimeMillis();
        mvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(KEY_HDR, key)
                        .content(body))
                .andExpect(status().isCreated());
        long elapsed = System.currentTimeMillis() - start;

        assertThat(elapsed).isLessThan(1_000L);
    }

    // ── User Story 3: Fraud/Error check (different body, same key) ───────────

    @Test
    @DisplayName("US3 — same key with different body returns 409 Conflict")
    void differentBodyReturns409() throws Exception {
        String key = uniqueKey();

        mvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(KEY_HDR, key)
                        .content("""
                                {"amount": 100, "currency": "GHS"}"""))
                .andExpect(status().isCreated());

        mvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(KEY_HDR, key)
                        .content("""
                                {"amount": 500, "currency": "GHS"}"""))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error")
                        .value("Idempotency key already used for a different request body."));
    }

    // ── Bonus: Race-condition (in-flight) check ───────────────────────────────

    @Test
    @DisplayName("BONUS — concurrent request with same key waits and returns same result")
    void concurrentRequestWaitsForInFlight() throws Exception {
        String key  = uniqueKey();
        String body = """
                {"amount": 300, "currency": "GHS"}""";

        // Fire first request asynchronously — it will hold the lock for ~2s
        CompletableFuture<MvcResult> future1 = CompletableFuture.supplyAsync(() -> {
            try {
                return mvc.perform(post(ENDPOINT)
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(KEY_HDR, key)
                                .content(body))
                        .andReturn();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        // Give the first request 300ms head-start to insert the IN_FLIGHT record
        Thread.sleep(300);

        // Fire second request — should block and then get the same result
        MvcResult result2 = mvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(KEY_HDR, key)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(header().string("X-Cache-Hit", "true"))
                .andReturn();

        MvcResult result1 = future1.get(15, TimeUnit.SECONDS);
        assertThat(result1.getResponse().getStatus()).isEqualTo(201);

        PaymentResponse r1 = mapper.readValue(result1.getResponse().getContentAsString(), PaymentResponse.class);
        PaymentResponse r2 = mapper.readValue(result2.getResponse().getContentAsString(), PaymentResponse.class);

        assertThat(r1.getTransactionId()).isEqualTo(r2.getTransactionId());
    }

    // ── HashUtil unit test ────────────────────────────────────────────────────

    @Test
    @DisplayName("HashUtil — same input always produces same SHA-256 hex")
    void hashUtilIsDeterministic() {
        String input = """
                {"amount": 100, "currency": "GHS"}""";
        String h1 = com.finsafe.gateway.config.HashUtil.sha256Hex(input);
        String h2 = com.finsafe.gateway.config.HashUtil.sha256Hex(input);
        assertThat(h1).isEqualTo(h2).hasSize(64);
    }

    @Test
    @DisplayName("HashUtil — different inputs produce different hashes")
    void hashUtilDetectsDifference() {
        String a = """
                {"amount": 100, "currency": "GHS"}""";
        String b = """
                {"amount": 500, "currency": "GHS"}""";
        assertThat(com.finsafe.gateway.config.HashUtil.sha256Hex(a))
                .isNotEqualTo(com.finsafe.gateway.config.HashUtil.sha256Hex(b));
    }

    // ── Utility ───────────────────────────────────────────────────────────────

    private String uniqueKey() {
        return "test-key-" + UUID.randomUUID();
    }
}