package idempotency

import idempotency.api.paymentRoutes
import idempotency.engine.PaymentEngine
import idempotency.store.IdempotencyStore
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.serialization.gson.gson
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import org.slf4j.event.Level

fun main() {
    val store = IdempotencyStore()
    val engine = PaymentEngine(store)

    embeddedServer(Netty, port = 8080, host = "0.0.0.0") {
        configure(engine)
    }.start(wait = true)
}

fun Application.configure(engine: PaymentEngine) {
    install(ContentNegotiation) {
        gson()
    }

    install(CORS) {
        anyHost()
        allowHeader(HttpHeaders.ContentType)
        allowHeader("Idempotency-Key")
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Get)
    }

    install(CallLogging) {
        level = Level.INFO
    }

    routing {
        get("/health") {
            call.respondText("OK")
        }

        paymentRoutes(engine)
    }
}
