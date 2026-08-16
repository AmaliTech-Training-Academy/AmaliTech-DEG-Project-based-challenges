package idempotency.store

import idempotency.model.PaymentResult
import java.util.concurrent.ConcurrentHashMap

class IdempotencyStore {

    private data class StoredEntry(
        val requestBody: String,
        val result: PaymentResult,
        val createdAt: Long = System.currentTimeMillis()
    )

    private val entries = ConcurrentHashMap<String, StoredEntry>()

    fun get(key: String): PaymentResult? {
        val entry = entries[key] ?: return null
        // check if expired (24 hours)
        if (System.currentTimeMillis() - entry.createdAt > 24 * 60 * 60 * 1000) {
            entries.remove(key)
            return null
        }
        return entry.result
    }

    fun getRequestBody(key: String): String? {
        return entries[key]?.requestBody
    }

    fun put(key: String, requestBody: String, result: PaymentResult) {
        entries[key] = StoredEntry(requestBody, result)
    }

    fun size(): Int = entries.size
}
