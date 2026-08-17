package idempotency.store

import idempotency.model.PaymentResult

class StoredEntry(
    val requestBody: String,
    val result: PaymentResult,
    val createdAt: Long = System.currentTimeMillis()
)
