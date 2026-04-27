package com.finsafe.gateway.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI finsafeOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("FinSafe Idempotency Gateway API")
                        .version("1.0.0")
                        .description("""
                                ## Pay-Once Protocol
                                
                                A production-grade idempotency layer that ensures every payment \
                                request is processed **exactly once**, regardless of how many \
                                times the client retries.
                                
                                ### How Idempotency Works
                                - Every request must include a unique `Idempotency-Key` header
                                - First request: payment is processed and response is stored
                                - Duplicate request (same key + same body): cached response \
                                is returned instantly with `X-Cache-Hit: true`
                                - Same key + different body: rejected with `409 Conflict`
                                - Concurrent duplicate while processing: waits and replays result
                                
                                ### Key Expiry (TTL)
                                Keys expire after **24 hours** by default. \
                                After expiry the key can be reused for a new payment.
                                """)
                        .contact(new Contact()
                                .name("FinSafe Engineering")
                                .email("engineering@finsafe.com"))
                        .license(new License()
                                .name("MIT License")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8080")
                                .description("Local development server")
                ));
    }
}