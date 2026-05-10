package com.finsafe.idempotency.dto;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class PaymentRequestDto {

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be greater than zero")
    private Double amount;

    @NotBlank(message = "Currency is required")
    private String currency;
}