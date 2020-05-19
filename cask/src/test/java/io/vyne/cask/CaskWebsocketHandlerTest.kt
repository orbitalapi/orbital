package io.vyne.cask

import com.nhaarman.mockito_kotlin.argumentCaptor
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import com.winterbe.expekt.should
import io.vyne.cask.format.json.CoinbaseJsonOrderSchema
import io.vyne.cask.ingest.Ingester
import io.vyne.cask.ingest.IngesterFactory
import io.vyne.cask.ingest.IngestionInitialisedEvent
import io.vyne.cask.ingest.IngestionStream
import io.vyne.cask.websocket.MockDataBuffer
import io.vyne.cask.websocket.MockWebSocketSession
import io.vyne.schemaStore.SchemaProvider
import io.vyne.schemas.Schema
import org.junit.Before
import org.junit.Test
import org.springframework.context.ApplicationEventPublisher
import org.springframework.web.reactive.socket.CloseStatus
import org.springframework.web.reactive.socket.WebSocketMessage
import reactor.core.publisher.Flux
import reactor.test.StepVerifier
import java.io.ByteArrayInputStream
import java.time.Duration


class CaskWebsocketHandlerTest {
   val ingester: Ingester = mock()
   val applicationEventPublisher = mock<ApplicationEventPublisher>()
   lateinit var wsHandler: CaskWebsocketHandler

   class IngesterFactoryMock(val ingester: Ingester) : IngesterFactory(mock()) {
      override fun create(ingestionStream: IngestionStream): Ingester {
         whenever(ingester.ingest()).thenReturn(ingestionStream.feed.stream)
         return ingester
      }
   }

   fun schemaProvider(): SchemaProvider {
      return object : SchemaProvider {
         override fun schemas(): List<Schema> = listOf(CoinbaseJsonOrderSchema.schemaV1)
      }
   }

   private val caskService = CaskService(schemaProvider(), IngesterFactoryMock(ingester))

   @Before()
   fun setUp() {
      wsHandler = CaskWebsocketHandler(caskService, applicationEventPublisher)
   }

   @Test
   fun closeWebsocketForUnknownContentType() {
      val session = MockWebSocketSession("/cask/OrderWindowSummary?contentType=testContentType")
      val wsHandler = CaskWebsocketHandler(caskService, applicationEventPublisher)

      wsHandler.handle(session)

      session.closed.should.be.`true`
      session.closeStatus.should.equal(CloseStatus(1003, "Unknown contentType=testContentType"))
   }

   @Test
   fun closeWebsocketForUnsupportedContentType() {
      val session = MockWebSocketSession("/cask/OrderWindowSummary?contentType=application/xml")

      wsHandler.handle(session)

      session.closed.should.be.`true`
      session.closeStatus.should.equal(CloseStatus(1003, "Unsupported contentType=application/xml"))
   }

   @Test
   fun closeWebsocketWhenTypeNotFound() {
      val session = MockWebSocketSession("/cask/OrderWindowSummary2")

      wsHandler.handle(session)

      session.closed.should.be.`true`
      session.closeStatus.should.equal(CloseStatus(1003, "Type reference 'OrderWindowSummary2' not found."))
   }

   @Test
   fun successfulIngestionResponseWhenDebugEnabled() {
      val sessionInput = Flux.just(WebSocketMessage(WebSocketMessage.Type.TEXT, MockDataBuffer(validIngestionMessage())))
      val session = MockWebSocketSession(uri = "/cask/OrderWindowSummary", input = sessionInput)
      val captor = argumentCaptor<IngestionInitialisedEvent>()

      wsHandler.handle(session).block()

      StepVerifier
         .create(session.textOutput.take(Duration.ofMillis(200)))
         .expectNextCount(0)
         .verifyComplete()
      verify(applicationEventPublisher, times(1)).publishEvent(captor.capture())
      "OrderWindowSummary".should.be.equal(captor.firstValue.type.fullyQualifiedName)
   }

   @Test
   fun noIngestionResponseWhenDebugDisabled() {
      val sessionInput = Flux.just(WebSocketMessage(WebSocketMessage.Type.TEXT, MockDataBuffer(validIngestionMessage())))
      val session = MockWebSocketSession(uri = "/cask/OrderWindowSummary?debug=true", input = sessionInput)
      val captor = argumentCaptor<IngestionInitialisedEvent>()

      wsHandler.handle(session).block()

      StepVerifier
         .create(session.textOutput.take(1))
         .expectNext("""{"result":"SUCCESS","message":"Successfully ingested 1 records"}""")
         .verifyComplete()
      verify(applicationEventPublisher, times(1)).publishEvent(captor.capture())
      "OrderWindowSummary".should.be.equal(captor.firstValue.type.fullyQualifiedName)
   }

   @Test
   fun unexpectedIngestionError() {
      val sessionInput = Flux.just(WebSocketMessage(WebSocketMessage.Type.TEXT, MockDataBuffer(validIngestionMessage())))
      val session = MockWebSocketSession(uri = "/cask/OrderWindowSummary", input = sessionInput)
      whenever(ingester.ingest()).thenThrow(RuntimeException("No database connection"))

      wsHandler.handle(session).block()

      StepVerifier
         .create(session.textOutput.take(1))
         .expectNext("""{"result":"REJECTED","message":"No database connection"}""")
         .verifyComplete()
   }

   @Test
   fun illegalArgumentExceptionError() {
      val sessionInput = Flux.just(WebSocketMessage(WebSocketMessage.Type.TEXT, MockDataBuffer(validIngestionMessage())))
      val session = MockWebSocketSession(uri = "/cask/OrderWindowSummary", input = sessionInput)
      whenever(ingester.ingest()).thenThrow(RuntimeException(null, IllegalArgumentException()))

      wsHandler.handle(session).block()

      StepVerifier
         .create(session.textOutput.take(1))
         .expectNext("""{"result":"REJECTED","message":"An IllegalArgumentException was thrown, but no further details are available."}""")
         .verifyComplete()
   }

   @Test
   fun ingestionErrorCausedByInvalidType() {
      val sessionInput = Flux.just(WebSocketMessage(WebSocketMessage.Type.TEXT, MockDataBuffer(invalidIngestionMessage())))
      val session = MockWebSocketSession(uri = "/cask/OrderWindowSummary", input = sessionInput)
      wsHandler.handle(session).block()

      StepVerifier
         .create(session.textOutput.take(1))
         .expectNext("""{"result":"REJECTED","message":"Cannot deserialize value of type `java.math.BigDecimal` from String \"6300USD\": not a valid representation\n at [Source: UNKNOWN; line: -1, column: -1]"}""")
         .verifyComplete()
   }

   @Test
   fun ingestionErrorCausedByMissingValue() {
      val sessionInput = Flux.just(WebSocketMessage(WebSocketMessage.Type.TEXT, MockDataBuffer(ingestionMessageWithMissingValue())))
      val session = MockWebSocketSession(uri = "/cask/OrderWindowSummary", input = sessionInput)
      wsHandler.handle(session).block()

      StepVerifier
         .create(session.textOutput.take(1))
         .expectNext("""{"result":"REJECTED","message":"Unable to parse primitive type=STRING name=Symbol value=null."}""")
         .verifyComplete()
   }

   @Test
   fun continueProcessingMessagesAfterError() {
      val malformedJson = WebSocketMessage(WebSocketMessage.Type.TEXT, MockDataBuffer(malformedJsonMessage()))
      val invalidType = WebSocketMessage(WebSocketMessage.Type.TEXT, MockDataBuffer(invalidIngestionMessage()))
      val validMessage = WebSocketMessage(WebSocketMessage.Type.TEXT, MockDataBuffer(validIngestionMessage()))
      val sessionInput = Flux.just(malformedJson, invalidType, validMessage)
      val session = MockWebSocketSession(uri = "/cask/OrderWindowSummary?debug=true", input = sessionInput)
      wsHandler.handle(session).block()

      StepVerifier
         .create(session.textOutput.take(3))
         .expectNext("""{"result":"REJECTED","message":"com.fasterxml.jackson.core.io.JsonEOFException: Unexpected end-of-input in VALUE_STRING\n at [Source: (ByteArrayInputStream); line: 1, column: 15]"}""")
         .expectNext("""{"result":"REJECTED","message":"Cannot deserialize value of type `java.math.BigDecimal` from String \"6300USD\": not a valid representation\n at [Source: UNKNOWN; line: -1, column: -1]"}""")
         .expectNext("""{"result":"SUCCESS","message":"Successfully ingested 1 records"}""")
         .verifyComplete()
   }

   private fun validIngestionMessage(): ByteArrayInputStream {
      return """{
        "Date": "2020-03-19",
        "Symbol": "BTCUSD",
        "Open": "6300",
        "High": "6330",
        "Low": "6186.08",
        "Close": "6235.2"
         }""".byteInputStream()
   }

   private fun invalidIngestionMessage(): ByteArrayInputStream {
      return """{
        "Date": "2020-03-19",
        "Symbol": "BTCUSD",
        "Open": "6300USD",
        "High": "6330",
        "Low": "6186.08",
        "Close": "6235.2"
         }""".byteInputStream()
   }

   private fun ingestionMessageWithMissingValue(): ByteArrayInputStream {
      return """{
        "Date": "2020-03-19",
        "Open": "6300",
        "High": "6330",
        "Low": "6186.08",
        "Close": "6235.2"
         }""".byteInputStream()
   }

   private fun malformedJsonMessage() = """{"Date": "2020""".byteInputStream()
}
