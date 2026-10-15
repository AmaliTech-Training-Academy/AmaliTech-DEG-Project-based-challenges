package idempotency.api

import com.google.gson.Gson
import com.google.gson.JsonObject
import idempotency.engine.PaymentEngine
import idempotency.model.PaymentRequest
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receiveText
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.post

fun Route.paymentRoutes(engine: PaymentEngine) {
    val gson = Gson()

    post("/process-payment") {
        val idempotencyKey = call.request.headers["Idempotency-Key"]
        if (idempotencyKey.isNullOrBlank()) {
            val json = JsonObject().apply {
                addProperty("error", "Missing Idempotency-Key header")
            }
            call.respondText(
                json.toString(),
                ContentType.Application.Json,
                HttpStatusCode.BadRequest
            )
            return@post
        }

        val body = call.receiveText()
        val paymentRequest = try {
            gson.fromJson(body, PaymentRequest::class.java)
        } catch (e: Exception) {
            val json = JsonObject().apply {
                addProperty("error", "Invalid request body")
            }
            call.respondText(
                json.toString(),
                ContentType.Application.Json,
                HttpStatusCode.BadRequest
            )
            return@post
        }

        if (paymentRequest == null) {
            val json = JsonObject().apply {
                addProperty("error", "Invalid request body")
            }
            call.respondText(
                json.toString(),
                ContentType.Application.Json,
                HttpStatusCode.BadRequest
            )
            return@post
        }

        val result = validatePaymentRequest(paymentRequest)
        if (result != null) {
            val json = JsonObject().apply {
                addProperty("error", result)
            }
            call.respondText(
                json.toString(),
                ContentType.Application.Json,
                HttpStatusCode.BadRequest
            )
            return@post
        }

        val engineResult = engine.processPayment(paymentRequest, idempotencyKey, body)

        call.response.headers.append("X-Cache-Hit", if (engineResult.cached) "true" else "false")

        val json = JsonObject().apply {
            addProperty("message", engineResult.message)
        }
        call.respondText(
            json.toString(),
            ContentType.Application.Json,
            HttpStatusCode.fromValue(engineResult.statusCode)
        )
    }
}

fun validatePaymentRequest(request: PaymentRequest): String? {
    if (request.amount <= 0) {
        return "Amount must be greater than 0"
    }
    if (request.currency.isBlank() || !request.currency.matches(Regex("^[A-Z]{3}$"))) {
        return "Currency must be a 3-letter uppercase code (e.g. RWF, USD)"
    }
    return null
}
