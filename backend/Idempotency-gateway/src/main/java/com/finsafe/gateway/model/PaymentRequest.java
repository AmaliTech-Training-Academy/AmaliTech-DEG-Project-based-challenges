package com.finsafe.gateway.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Payment request body")
public class PaymentRequest {

    @Schema(
            description = "Payment amount — must be greater than zero",
            example = "100.0",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotNull(message = "amount is required")
    @DecimalMin(value = "0.01", message = "amount must be greater than 0")
    private Double amount;

    @Schema(
            description = "ISO 4217 currency code or any currency string",
            example = "GHS",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "currency is required")
    private String currency;

    public Double getAmount()           { return amount; }
    public void   setAmount(Double a)   { this.amount = a; }
    public String getCurrency()         { return currency; }
    public void   setCurrency(String c) { this.currency = c; }
}