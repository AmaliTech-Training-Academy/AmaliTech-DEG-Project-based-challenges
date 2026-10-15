package idempotency.store

import idempotency.model.PaymentResult
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class IdempotencyStore {

    private val ttlMillis = 24 * 60 * 60 * 1000L
    private val cleanupIntervalMinutes = 60L

    private val entries = ConcurrentHashMap<String, StoredEntry>()
    private val scheduler = Executors.newSingleThreadScheduledExecutor { runnable ->
        val thread = Thread(runnable)
        thread.isDaemon = true
        thread.name = "idempotency-cleanup"
        thread
    }

    fun startCleanupThread() {
        scheduler.scheduleAtFixedRate(
            { cleanup() },
            cleanupIntervalMinutes,
            cleanupIntervalMinutes,
            TimeUnit.MINUTES
        )
    }

    fun stopCleanupThread() {
        scheduler.shutdown()
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow()
            }
        } catch (e: InterruptedException) {
            scheduler.shutdownNow()
        }
    }

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

    fun cleanup(): Int {
        var removed = 0
        val now = System.currentTimeMillis()
        val iterator = entries.entries.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (now - entry.value.createdAt > ttlMillis) {
                iterator.remove()
                removed++
            }
        }
        return removed
    }

    private fun isExpired(entry: StoredEntry): Boolean {
        return System.currentTimeMillis() - entry.createdAt > ttlMillis
    }
}
