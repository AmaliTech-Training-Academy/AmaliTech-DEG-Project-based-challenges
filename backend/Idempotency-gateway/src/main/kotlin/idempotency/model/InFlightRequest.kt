package idempotency.model

import java.util.concurrent.CountDownLatch

class InFlightRequest {
    val latch = CountDownLatch(1)
    var result: PaymentResult? = null
}
