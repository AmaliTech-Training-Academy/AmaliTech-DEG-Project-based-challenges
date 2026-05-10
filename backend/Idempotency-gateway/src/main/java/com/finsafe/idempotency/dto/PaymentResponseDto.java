package com.finsafe.idempotency.dto;


import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponseDto {

    private String status;
    private String message;
    private Double amount;
    private String currency;
    private String idempotencyKey;
}