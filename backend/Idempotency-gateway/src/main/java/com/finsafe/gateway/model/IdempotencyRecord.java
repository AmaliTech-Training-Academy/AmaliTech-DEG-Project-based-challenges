package com.finsafe.gateway.model;

import java.time.Instant;

/**
 * Represents one row in the idempotency_records table.
 * <p>
 * status lifecycle:  IN_FLIGHT  →  DONE
 */
public class IdempotencyRecord {

    public enum Status { IN_FLIGHT, DONE }

    private String  idempotencyKey;
    private String  bodyHash;        // SHA-256 hex of the original request body
    private Status  status;
    private String  responseBody;    // serialised JSON response, null while IN_FLIGHT
    private Integer responseStatus;  // HTTP status code, null while IN_FLIGHT
    private Instant createdAt;
    private Instant expiresAt;

    public IdempotencyRecord() {}

    // ── Getters & Setters ────────────────────────────────────────────────────

    public String getIdempotencyKey()          { return idempotencyKey; }
    public void   setIdempotencyKey(String k)  { this.idempotencyKey = k; }

    public String getBodyHash()                { return bodyHash; }
    public void   setBodyHash(String h)        { this.bodyHash = h; }

    public Status getStatus()                  { return status; }
    public void   setStatus(Status s)          { this.status = s; }

    public String getResponseBody()            { return responseBody; }
    public void   setResponseBody(String rb)   { this.responseBody = rb; }

    public Integer getResponseStatus()         { return responseStatus; }
    public void    setResponseStatus(Integer c){ this.responseStatus = c; }

    public Instant getCreatedAt()              { return createdAt; }
    public void    setCreatedAt(Instant t)     { this.createdAt = t; }

    public Instant getExpiresAt()              { return expiresAt; }
    public void    setExpiresAt(Instant t)     { this.expiresAt = t; }

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }
}