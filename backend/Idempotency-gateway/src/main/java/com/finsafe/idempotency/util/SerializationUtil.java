package com.finsafe.idempotency.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finsafe.idempotency.dto.PaymentResponseDto;

public class SerializationUtil {

    private static final ObjectMapper mapper = new ObjectMapper();

    public static String stringify(PaymentResponseDto response) {
        try {
            return mapper.writeValueAsString(response);
        } catch (Exception e) {
            throw new RuntimeException("Failed to stringify response", e);
        }
    }

    public static PaymentResponseDto parse(String json) {
        try {
            return mapper.readValue(json, PaymentResponseDto.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse response", e);
        }
    }
}