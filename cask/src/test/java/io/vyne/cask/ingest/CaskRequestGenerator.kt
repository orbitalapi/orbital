package io.vyne.cask.ingest

import com.fasterxml.jackson.databind.PropertyNamingStrategy
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.vyne.utils.log
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient
import org.springframework.web.reactive.socket.client.WebSocketClient
import reactor.core.publisher.Flux
import java.net.URI
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.util.*


/**
 * How to use this client
 * 1. Start vyne and schema file server
 * 2. Start cask
 * 3. select number of messages  per second
 * 4. Start this client
 * 5. Go to http://localhost:8800/actuator/prometheus and observe cask_ingestion_request_seconds metric
 */
class CaskRequestGenerator {
   companion object {
      @JvmStatic
      fun main(args: Array<String>) {
         val noOfRequestsPerSecond = 10
         val client: WebSocketClient = ReactorNettyWebSocketClient()
         client.execute(URI.create("ws://localhost:8800/cask/OrderWindowSummary")) { session ->
            val requestIntervalInMs = Duration.ofMillis(1000L / noOfRequestsPerSecond)
            val requestStream = Flux.interval(requestIntervalInMs)
               .map { generateCaskRequest() }
               .doOnNext { log().info("${Instant.now()} sending request ${it}") }
               .map { session.textMessage(it) }
            session.send(requestStream).then()
         }.block()
         // Connects to cask endpoint and pushes messages at pre-defined interval
      }

      private fun generateCaskRequest(): String? {
         val orders = generateOrders()
         val mapper = jacksonObjectMapper()
                  .registerModule(JavaTimeModule())
                  // enforcing property names starting with uppercase letter
                  .setPropertyNamingStrategy(PropertyNamingStrategy.UPPER_CAMEL_CASE);
          return mapper.writeValueAsString(orders)
      }

      private fun generateOrders(): List<OrderWindowSummary> {
         val random = Random()
         return (0..10).map {
            OrderWindowSummary(
               LocalDate.now().minusDays(1L * random.nextInt(10)),
               currencyPairs[random.nextInt(5)],
               1 + random.nextDouble(),
               1 + random.nextDouble(),
               1 + random.nextDouble(),
               1 + random.nextDouble()
            )
         }
      }

      val currencyPairs = listOf("GBPUSD", "PLNUSD", "EURUSD", "CHFUSD", "JPYUSD")

      data class OrderWindowSummary(
         val Date: LocalDate,
         val Symbol: String,
         val Open: Double,
         val Close: Double,
         val High: Double,
         val Low: Double
      )
   }
}
