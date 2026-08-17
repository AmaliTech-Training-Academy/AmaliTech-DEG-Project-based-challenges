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

        val result = engine.processPayment(paymentRequest, idempotencyKey, body)

        call.response.headers.append("X-Cache-Hit", if (result.cached) "true" else "false")

        val json = JsonObject().apply {
            addProperty("message", result.message)
        }
        call.respondText(
            json.toString(),
            ContentType.Application.Json,
            HttpStatusCode.fromValue(result.statusCode)
        )
    }
}
