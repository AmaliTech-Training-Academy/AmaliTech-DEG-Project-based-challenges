package idempotency.engine

import idempotency.model.PaymentRequest
import idempotency.model.PaymentResult
import idempotency.store.IdempotencyStore
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicReference

class PaymentEngine(private val store: IdempotencyStore) {

    // track in-flight requests so concurrent ones can wait
    private val inFlight = ConcurrentHashMap<String, CountDownLatch>()
    private val inFlightResult = ConcurrentHashMap<String, AtomicReference<PaymentResult>>()

    fun processPayment(body: PaymentRequest, idempotencyKey: String): PaymentResult {
        // check if there is already an in-flight request for this key
        val latch = inFlight[idempotencyKey]
        if (latch != null) {
            // wait for the first request to finish and return its result
            latch.await()
            return inFlightResult[idempotencyKey]?.get() ?: PaymentResult("Error", 500)
        }

        // check if we already have a stored result for this key
        val cachedResult = store.get(idempotencyKey)
        if (cachedResult != null) {
            // check if the request body matches
            val storedBody = store.getRequestBody(idempotencyKey)
            if (storedBody != null && storedBody != body.toString()) {
                return PaymentResult(
                    message = "Idempotency key already used for a different request body.",
                    statusCode = 422
                )
            }
            return cachedResult
        }

        // first request - process it
        val newLatch = CountDownLatch(1)
        inFlight[idempotencyKey] = newLatch
        inFlightResult[idempotencyKey] = AtomicReference()

        try {
            // simulate processing delay (2 seconds)
            Thread.sleep(2000)

            val result = PaymentResult(
                message = "Charged ${body.amount} ${body.currency}",
                statusCode = 200
            )

            // store the result for future duplicate checks
            store.put(idempotencyKey, body.toString(), result)
            inFlightResult[idempotencyKey]?.set(result)
            newLatch.countDown()

            return result
        } finally {
            inFlight.remove(idempotencyKey)
            inFlightResult.remove(idempotencyKey)
        }
    }
}
