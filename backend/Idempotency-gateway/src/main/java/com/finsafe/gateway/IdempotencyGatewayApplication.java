package com.finsafe.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class IdempotencyGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(IdempotencyGatewayApplication.class, args);
    }
}