package com.finsafe.gateway.model;

/**
 * Outgoing payment response body.
 * Example JSON: {"status": "Charged 100.0 GHS", "transactionId": "txn_abc123"}
 */
public class PaymentResponse {

    private String status;
    private String transactionId;

    public PaymentResponse() {}

    public PaymentResponse(String status, String transactionId) {
        this.status = status;
        this.transactionId = transactionId;
    }

    public String getStatus()               { return status; }
    public void   setStatus(String s)       { this.status = s; }

    public String getTransactionId()        { return transactionId; }
    public void   setTransactionId(String t){ this.transactionId = t; }
}