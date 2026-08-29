package com.finsafe.idempotency;

import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

@SpringBootTest
@AutoConfigureMockMvc
class PaymentControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @AfterEach
    void pauseBetweenTests() throws InterruptedException {
        Thread.sleep(50L);
    }

    @Test
    void shouldProcessFirstRequestAndReplayOnDuplicate() throws Exception {
        String key = "order-abc-001";
        String body = "{\"amount\":100,\"currency\":\"GHS\"}";

        long firstStart = System.currentTimeMillis();
        MvcResult first = performRequest(key, body);
        long firstElapsed = System.currentTimeMillis() - firstStart;

        long secondStart = System.currentTimeMillis();
        MvcResult second = performRequest(key, body);
        long secondElapsed = System.currentTimeMillis() - secondStart;

        Assertions.assertTrue(firstElapsed >= 1800, "Initial request should include simulated processing delay.");
        Assertions.assertTrue(secondElapsed < 1000, "Duplicate request should return quickly.");
        Assertions.assertEquals(first.getResponse().getStatus(), second.getResponse().getStatus());
        Assertions.assertEquals(first.getResponse().getContentAsString(), second.getResponse().getContentAsString());
        Assertions.assertEquals("true", second.getResponse().getHeader("X-Cache-Hit"));
    }

    @Test
    void shouldRejectSameIdempotencyKeyWithDifferentPayload() throws Exception {
        String key = "order-conflict-001";
        performRequest(key, "{\"amount\":100,\"currency\":\"GHS\"}");

        mockMvc.perform(MockMvcRequestBuilders.post("/process-payment")
                        .header("Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":500,\"currency\":\"GHS\"}"))
                .andExpect(MockMvcResultMatchers.status().isUnprocessableEntity())
                .andExpect(MockMvcResultMatchers.content().json("{\"error\":\"Idempotency key already used for a different request body.\"}"));
    }

    @Test
    void shouldWaitForInFlightRequestInsteadOfDoubleProcessing() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch startLatch = new CountDownLatch(1);
        String key = "order-race-001";
        String body = "{\"amount\":700,\"currency\":\"GHS\"}";

        Callable<ResultWithTime> task = () -> {
            startLatch.await(2, TimeUnit.SECONDS);
            long start = System.currentTimeMillis();
            MvcResult result = performRequest(key, body);
            long elapsed = System.currentTimeMillis() - start;
            return new ResultWithTime(result, elapsed);
        };

        Future<ResultWithTime> firstFuture = executor.submit(task);
        Future<ResultWithTime> secondFuture = executor.submit(task);

        startLatch.countDown();
        ResultWithTime first = firstFuture.get(10, TimeUnit.SECONDS);
        ResultWithTime second = secondFuture.get(10, TimeUnit.SECONDS);
        executor.shutdownNow();

        Assertions.assertEquals(first.result.getResponse().getContentAsString(), second.result.getResponse().getContentAsString());
        Assertions.assertTrue(first.elapsed >= 1800 || second.elapsed >= 1800,
                "At least one request should include processing delay.");
        Assertions.assertTrue(first.elapsed >= 1800 && second.elapsed >= 1800,
                "In-flight duplicate should block until processing completes.");
    }

    private MvcResult performRequest(String key, String jsonBody) throws Exception {
        return mockMvc.perform(MockMvcRequestBuilders.post("/process-payment")
                        .header("Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBody))
                .andExpect(MockMvcResultMatchers.status().isCreated())
                .andExpect(MockMvcResultMatchers.content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andReturn();
    }

    private record ResultWithTime(MvcResult result, long elapsed) {
    }
}
