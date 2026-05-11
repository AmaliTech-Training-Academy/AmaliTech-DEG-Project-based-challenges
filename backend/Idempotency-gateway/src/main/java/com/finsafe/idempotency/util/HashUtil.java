package com.finsafe.idempotency.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finsafe.idempotency.dto.PaymentRequestDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

@Component
@RequiredArgsConstructor
public class HashUtil {

    private final ObjectMapper objectMapper;

    public String hashPayload(PaymentRequestDto request) {
        try {
            String json = objectMapper.writeValueAsString(request);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(json.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new RuntimeException("Failed to hash payload", e);
        }
    }
}