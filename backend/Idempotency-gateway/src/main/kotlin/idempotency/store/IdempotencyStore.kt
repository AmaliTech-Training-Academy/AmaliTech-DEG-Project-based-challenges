package idempotency.store

import idempotency.model.PaymentResult
import java.util.concurrent.ConcurrentHashMap

class IdempotencyStore {

    private val ttlMillis = 24 * 60 * 60 * 1000L

    private val entries = ConcurrentHashMap<String, StoredEntry>()

    fun get(key: String): PaymentResult? {
        val entry = entries[key] ?: return null
        if (isExpired(entry)) {
            entries.remove(key)
            return null
        }
        return entry.result
    }

    fun getRequestBody(key: String): String? {
        val entry = entries[key] ?: return null
        if (isExpired(entry)) {
            entries.remove(key)
            return null
        }
        return entry.requestBody
    }

    fun put(key: String, requestBody: String, result: PaymentResult) {
        entries[key] = StoredEntry(requestBody, result)
    }

    fun size(): Int = entries.size

    private fun isExpired(entry: StoredEntry): Boolean {
        return System.currentTimeMillis() - entry.createdAt > ttlMillis
    }
}
