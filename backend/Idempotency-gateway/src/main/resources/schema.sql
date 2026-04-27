-- Enable WAL mode for better concurrent read/write performance
PRAGMA journal_mode=WAL;
PRAGMA busy_timeout=5000;

CREATE TABLE IF NOT EXISTS idempotency_records (
                                                   idempotency_key  TEXT    NOT NULL PRIMARY KEY,
                                                   body_hash        TEXT    NOT NULL,
                                                   status           TEXT    NOT NULL DEFAULT 'IN_FLIGHT',
                                                   response_body    TEXT,
                                                   response_status  INTEGER,
                                                   created_at       TEXT    NOT NULL,
                                                   expires_at       TEXT    NOT NULL
);