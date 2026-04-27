package com.finsafe.gateway.repository;

import com.finsafe.gateway.model.IdempotencyRecord;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Data-access layer for idempotency_records.
 * Uses plain Spring JdbcTemplate to keep things transparent and SQLite-friendly.
 */
@Repository
public class IdempotencyRepository {

    private final JdbcTemplate jdbc;

    public IdempotencyRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // ── Read ─────────────────────────────────────────────────────────────────

    public Optional<IdempotencyRecord> findByKey(String key) {
        String sql = "SELECT * FROM idempotency_records WHERE idempotency_key = ?";
        List<IdempotencyRecord> rows = jdbc.query(sql, new RecordRowMapper(), key);
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
    }

    // ── Write ─────────────────────────────────────────────────────────────────

    /**
     * Insert a brand-new IN_FLIGHT record.
     * Called before payment processing starts to claim the key and prevent races.
     */
    public void insertInFlight(String key, String bodyHash, Instant createdAt, Instant expiresAt) {
        String sql = """
                INSERT INTO idempotency_records
                    (idempotency_key, body_hash, status, created_at, expires_at)
                VALUES (?, ?, 'IN_FLIGHT', ?, ?)
                """;
        jdbc.update(sql, key, bodyHash, createdAt.toString(), expiresAt.toString());
    }

    /**
     * Transition an IN_FLIGHT record to DONE once processing completes.
     */
    public void markDone(String key, String responseBody, int responseStatus) {
        String sql = """
                UPDATE idempotency_records
                SET status = 'DONE', response_body = ?, response_status = ?
                WHERE idempotency_key = ?
                """;
        jdbc.update(sql, responseBody, responseStatus, key);
    }

    /**
     * Delete a record — used when processing fails so the key can be retried.
     */
    public void delete(String key) {
        jdbc.update("DELETE FROM idempotency_records WHERE idempotency_key = ?", key);
    }

    /**
     * Developer's Choice: purge all records whose expires_at is in the past.
     * Called by a scheduled task so the SQLite file doesn't grow unbounded.
     */
    public int deleteExpired() {
        String sql = "DELETE FROM idempotency_records WHERE expires_at < ?";
        return jdbc.update(sql, Instant.now().toString());
    }

    // ── RowMapper ─────────────────────────────────────────────────────────────

    private static class RecordRowMapper implements RowMapper<IdempotencyRecord> {
        @Override
        public IdempotencyRecord mapRow(ResultSet rs, int rowNum) throws SQLException {
            IdempotencyRecord r = new IdempotencyRecord();
            r.setIdempotencyKey(rs.getString("idempotency_key"));
            r.setBodyHash(rs.getString("body_hash"));
            r.setStatus(IdempotencyRecord.Status.valueOf(rs.getString("status")));
            r.setResponseBody(rs.getString("response_body"));

            int code = rs.getInt("response_status");
            r.setResponseStatus(rs.wasNull() ? null : code);

            r.setCreatedAt(Instant.parse(rs.getString("created_at")));
            r.setExpiresAt(Instant.parse(rs.getString("expires_at")));
            return r;
        }
    }
}