package idempotency.model

class PaymentResult(
    val message: String,
    val statusCode: Int,
    val cached: Boolean = false
)
