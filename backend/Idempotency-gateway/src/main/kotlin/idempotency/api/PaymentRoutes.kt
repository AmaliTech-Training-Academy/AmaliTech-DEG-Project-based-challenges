package idempotency.api

import com.google.gson.Gson
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
            call.respondText(
                """{"error": "Missing Idempotency-Key header"}""",
                ContentType.Application.Json,
                HttpStatusCode.BadRequest
            )
            return@post
        }

        val body = call.receiveText()
        val paymentRequest = try {
            gson.fromJson(body, PaymentRequest::class.java)
        } catch (e: Exception) {
            call.respondText(
                """{"error": "Invalid request body"}""",
                ContentType.Application.Json,
                HttpStatusCode.BadRequest
            )
            return@post
        }

        val result = engine.processPayment(paymentRequest, idempotencyKey)

        // set X-Cache-Hit header if this was a cached duplicate
        if (result.cached) {
            call.response.headers.append("X-Cache-Hit", "true")
        }

        call.respondText(
            """{"message": "${result.message}"}""",
            ContentType.Application.Json,
            HttpStatusCode.fromValue(result.statusCode)
        )
    }
}
