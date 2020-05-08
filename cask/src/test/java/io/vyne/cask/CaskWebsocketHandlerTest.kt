package io.vyne.cask

import arrow.core.Either
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.eq
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.whenever
import com.winterbe.expekt.should
import io.vyne.cask.format.json.CoinbaseJsonOrderSchema
import io.vyne.cask.websocket.MockDataBuffer
import io.vyne.cask.websocket.MockWebSocketSession
import io.vyne.schemas.fqn
import org.junit.Test
import org.springframework.web.reactive.socket.CloseStatus
import org.springframework.web.reactive.socket.WebSocketMessage
import reactor.core.publisher.Flux
import java.io.ByteArrayInputStream


class CaskWebsocketHandlerTest {
    val caskService = mock<CaskService>()

    @Test
    fun closeWebsocketWhenTypeNotFound() {
        val expectedErrorMessage = "Type not found"
        whenever(caskService.resolveType("OrderWindowSummary")).thenReturn(Either.right(CaskService.TypeError(expectedErrorMessage)))
        val session = MockWebSocketSession("/cask/OrderWindowSummary")
        val wsHandler = CaskWebsocketHandler(caskService)

        wsHandler.handle(session)

        session.closed.should.be.`true`
        session.closeStatus.should.equal(CloseStatus(1003, expectedErrorMessage))
    }

    @Test
    fun successfulIngestionResponse() {
        val versionedType = CoinbaseJsonOrderSchema.schemaV1.versionedType("OrderWindowSummary".fqn())
        whenever(caskService.resolveType("OrderWindowSummary")).thenReturn(Either.left(versionedType))
        whenever(caskService.ingestRequest(eq(versionedType), any())).thenReturn(Flux.empty())

        val sessionInput = Flux.just(WebSocketMessage(WebSocketMessage.Type.TEXT, MockDataBuffer(ingestionStream())))
        val session = MockWebSocketSession(uri = "/cask/OrderWindowSummary", input = sessionInput)
        val wsHandler = CaskWebsocketHandler(caskService)

        wsHandler.handle(session).block()

        session.textOutput.should.equal(
                """{"result":"SUCCESS","message":"Successfully ingested 0 records"}""".trimIndent())
    }

    @Test
    fun ingestionErrorResponse() {
        val versionedType = CoinbaseJsonOrderSchema.schemaV1.versionedType("OrderWindowSummary".fqn())
        whenever(caskService.resolveType("OrderWindowSummary")).thenReturn(Either.left(versionedType))
        whenever(caskService.ingestRequest(eq(versionedType), any())).thenReturn(Flux.error(RuntimeException("JSON ingestion error")))

        val sessionInput = Flux.just(WebSocketMessage(WebSocketMessage.Type.TEXT, MockDataBuffer(ingestionStream())))
        val session = MockWebSocketSession(uri = "/cask/OrderWindowSummary", input = sessionInput)
        val wsHandler = CaskWebsocketHandler(caskService)

        wsHandler.handle(session).block()

        session.textOutput.should.equal(
                """{"result":"REJECTED","message":"Error ingesting message"}""".trimIndent())
    }

    private fun ingestionStream(): ByteArrayInputStream {
        return """{
        "Date": "2020-03-19 11-PM",
        "Symbol": "BTCUSD",
        "Open": "6300",
        "High": "6330",
        "Low": "6186.08",
        "Close": "6235.2",
        "Volume BTC": "817.78",
        "Volume USD": "5115937.58"
         }""".byteInputStream()
    }
}