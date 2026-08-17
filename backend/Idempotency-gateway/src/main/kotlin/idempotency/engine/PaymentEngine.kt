package idempotency.engine

import idempotency.model.InFlightRequest
import idempotency.model.PaymentRequest
import idempotency.model.PaymentResult
import idempotency.store.IdempotencyStore
import java.util.concurrent.ConcurrentHashMap

class PaymentEngine(private val store: IdempotencyStore) {

    private val inFlight = ConcurrentHashMap<String, InFlightRequest>()

    fun isCached(key: String): Boolean {
        return store.get(key) != null
    }

    fun processPayment(body: PaymentRequest, idempotencyKey: String): PaymentResult {
        val existing = inFlight[idempotencyKey]
        if (existing != null) {
            existing.latch.await()
            return existing.result ?: PaymentResult("Error", 500)
        }

        synchronized(idempotencyKey.intern()) {
            val cachedResult = store.get(idempotencyKey)
            if (cachedResult != null) {
                val storedBody = store.getRequestBody(idempotencyKey)
                if (storedBody != null && storedBody != body.toString()) {
                    return PaymentResult(
                        "Idempotency key already used for a different request body.",
                        422
                    )
                }
                return PaymentResult(cachedResult.message, cachedResult.statusCode, true)
            }

            val inFlightReq = InFlightRequest()
            inFlight[idempotencyKey] = inFlightReq

            try {
                Thread.sleep(2000)

                val result = PaymentResult("Charged ${body.amount} ${body.currency}", 200)
                store.put(idempotencyKey, body.toString(), result)
                inFlightReq.result = result
                inFlightReq.latch.countDown()

                return result
            } finally {
                inFlight.remove(idempotencyKey)
            }
        }
    }
}
