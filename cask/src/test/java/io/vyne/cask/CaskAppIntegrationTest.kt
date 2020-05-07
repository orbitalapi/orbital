package io.vyne.cask

import io.vyne.cask.format.json.CoinbaseJsonOrderSchema
import io.vyne.schemaStore.SchemaProvider
import io.vyne.schemas.Schema
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.test.context.junit4.SpringRunner
import org.springframework.web.reactive.socket.WebSocketMessage
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient
import org.springframework.web.reactive.socket.client.WebSocketClient
import reactor.core.publisher.EmitterProcessor
import reactor.core.publisher.Mono
import java.net.URI


@RunWith(SpringRunner::class)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = [
            "spring.main.allow-bean-definition-overriding=true",
            "eureka.client.enabled=false"
        ])
class CaskAppIntegrationTest {

    @LocalServerPort
    val randomServerPort = 0

    @Test
    fun contextLoads() {
    }

    @TestConfiguration
    class SpringConfig {
        @Bean
        @Primary
        fun schemaProvider(): SchemaProvider {
            return object : SchemaProvider {
                override fun schemas(): List<Schema> = listOf(CoinbaseJsonOrderSchema.schemaV1)
            }
        }
    }

    @Test
    @Ignore("LENS-44 Run local Postgres db as part of integration tests")
    fun testWebsocket() {
        val output: EmitterProcessor<String> = EmitterProcessor.create()
        val client: WebSocketClient = ReactorNettyWebSocketClient()
        val uri = URI.create("ws://localhost:${randomServerPort}/cask/OrderWindowSummary")
        val caskRequest = """{
        "Date": "2020-03-19 11-PM",
        "Symbol": "BTCUSD",
        "Open": "6300",
        "High": "6330",
        "Low": "6186.08",
        "Close": "6235.2",
        "Volume BTC": "817.78",
        "Volume USD": "5115937.58"
         }""".trimIndent()

        client.execute(uri)
        { session ->
            session.send(Mono.just(session.textMessage(caskRequest)))
                    .thenMany(session.receive()
                            .log()
                            .map(WebSocketMessage::getPayloadAsText)
                            .subscribeWith(output))
                    .then()
        }.subscribe()

        output.blockFirst()
    }
}
