package idempotency.model

data class PaymentRequest(
    val amount: Int,
    val currency: String
)

data class PaymentResult(
    val message: String,
    val statusCode: Int
)
