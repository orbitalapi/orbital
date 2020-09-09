package io.vyne.cask.websocket

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.nhaarman.mockito_kotlin.argumentCaptor
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import com.winterbe.expekt.should
import io.vyne.cask.CaskService
import io.vyne.cask.api.CaskIngestionResponse
import io.vyne.cask.format.json.CoinbaseJsonOrderSchema
import io.vyne.cask.ingest.Ingester
import io.vyne.cask.ingest.IngesterFactory
import io.vyne.cask.ingest.IngestionInitialisedEvent
import io.vyne.cask.ingest.IngestionStream
import io.vyne.cask.query.CaskDAO
import io.vyne.schemaStore.SchemaProvider
import io.vyne.schemas.Schema
import io.vyne.schemas.VersionedType
import org.junit.Before
import org.junit.Test
import org.springframework.context.ApplicationEventPublisher
import org.springframework.core.io.buffer.DefaultDataBufferFactory
import org.springframework.web.reactive.socket.CloseStatus
import org.springframework.web.reactive.socket.WebSocketMessage
import reactor.core.publisher.Flux
import reactor.test.StepVerifier
import java.io.ByteArrayInputStream
import java.nio.file.Path
import java.nio.file.Paths
import java.time.Duration


class CaskWebsocketHandlerTest {
   val ingester: Ingester = mock()
   val caskDao: CaskDAO = mock()
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
         override fun schemas(): List<Schema> = listOf(CoinbaseJsonOrderSchema.nullableSchemaV1)
      }
   }

   private val caskService = CaskService(schemaProvider(), IngesterFactoryMock(ingester), caskDao)
   private val mapper: ObjectMapper = jacksonObjectMapper()


   @Before()
   fun setUp() {
      mapper.enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
      wsHandler = CaskWebsocketHandler(caskService, applicationEventPublisher, mapper)
   }

   @Test
   fun closeWebsocketForUnknownContentType() {
      val session = MockWebSocketSession("/cask/xxx/OrderWindowSummary")
      val wsHandler = CaskWebsocketHandler(caskService, applicationEventPublisher, mapper)

      wsHandler.handle(session)

      session.closed.should.be.`true`
      session.closeStatus.should.equal(CloseStatus(1003, "Unknown contentType=xxx"))
   }

   @Test
   fun closeWebsocketWhenTypeNotFound() {
      val session = MockWebSocketSession("/cask/json/OrderWindowSummary2")

      wsHandler.handle(session)

      session.closed.should.be.`true`
      session.closeStatus.should.equal(CloseStatus(1003, "Type reference 'OrderWindowSummary2' not found."))
   }

   @Test
   fun successfulIngestionResponseWhenDebugEnabled() {
      val sessionInput = Flux.just(WebSocketMessage(WebSocketMessage.Type.TEXT,
         DefaultDataBufferFactory().wrap(validIngestionMessage().readBytes())))
      val session = MockWebSocketSession(uri = "/cask/OrderWindowSummary", input = sessionInput)
      val captor = argumentCaptor<IngestionInitialisedEvent>()
      val versionedType = argumentCaptor<VersionedType>()
      val cachePath = argumentCaptor<Path>()
      val messageId = argumentCaptor<String>()

      wsHandler.handle(session).block()

      StepVerifier
         .create(session.textOutput.take(Duration.ofMillis(200)))
         .expectNextCount(0)
         .verifyComplete()
      verify(applicationEventPublisher, times(1)).publishEvent(captor.capture())
      "OrderWindowSummary".should.be.equal(captor.firstValue.type.fullyQualifiedName)

      verify(caskDao, times(1)).createCaskMessage(versionedType.capture(), cachePath.capture(), messageId.capture())
      val expectedPath = Paths.get(System.getProperty("java.io.tmpdir"), versionedType.firstValue.versionedName, "json", messageId.firstValue)
      cachePath.firstValue.should.be.equal(expectedPath)
   }

   @Test
   fun successfulJsonMessageIngestion() {
      val validMessage = WebSocketMessage(WebSocketMessage.Type.TEXT,
         DefaultDataBufferFactory().wrap(validIngestionMessage().readBytes()))

      val sessionInput = Flux.just(validMessage)
      val session = MockWebSocketSession(uri = "/cask/json/OrderWindowSummary?debug=true", input = sessionInput)
      wsHandler.handle(session).block()

      StepVerifier
         .create(session.textOutput.take(1).timeout(Duration.ofSeconds(1)))
         .expectNext("""{"result":"SUCCESS","message":"Successfully ingested 1 records"}""")
         .verifyComplete()
   }

   @Test
   fun noIngestionResponseWhenDebugDisabled() {
      val sessionInput = Flux.just(WebSocketMessage(WebSocketMessage.Type.TEXT,
         DefaultDataBufferFactory().wrap(validIngestionMessage().readBytes())))
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
      val sessionInput = Flux.just(WebSocketMessage(WebSocketMessage.Type.TEXT,
         DefaultDataBufferFactory().wrap(validIngestionMessage().readBytes())))
      val session = MockWebSocketSession(uri = "/cask/json/OrderWindowSummary", input = sessionInput)
      whenever(ingester.ingest()).thenThrow(RuntimeException("No database connection"))

      wsHandler.handle(session).block()

      StepVerifier
         .create(session.textOutput.take(1))
         .expectNext("""{"result":"REJECTED","message":"No database connection"}""")
         .verifyComplete()
   }

   @Test
   fun illegalArgumentExceptionError() {
      val sessionInput = Flux.just(WebSocketMessage(WebSocketMessage.Type.TEXT, DefaultDataBufferFactory().wrap(validIngestionMessage().readBytes())))
      val session = MockWebSocketSession(uri = "/cask/json/OrderWindowSummary", input = sessionInput)
      whenever(ingester.ingest()).thenThrow(RuntimeException(null, IllegalArgumentException()))

      wsHandler.handle(session).block()

      StepVerifier
         .create(session.textOutput.take(1))
         .expectNext("""{"result":"REJECTED","message":"An IllegalArgumentException was thrown, but no further details are available."}""")
         .verifyComplete()
   }

   @Test
   fun ingestionErrorCausedByInvalidType() {
      val sessionInput = Flux.just(WebSocketMessage(WebSocketMessage.Type.TEXT, DefaultDataBufferFactory().wrap(invalidIngestionMessage().readBytes())))
      val session = MockWebSocketSession(uri = "/cask/json/OrderWindowSummary", input = sessionInput)
      wsHandler.handle(session).block()

      StepVerifier
         .create(session.textOutput.take(1))
         .expectNextMatches { json -> json.rejectedWithReason("Failed to parse value 6300USD to type Price - Failed to convert from type [java.lang.String] to type [java.math.BigDecimal] for value '6300USD'") }
         .verifyComplete()
   }

   @Test
   fun ingestionErrorCausedByMissingValue() {
      val sessionInput = Flux.just(WebSocketMessage(WebSocketMessage.Type.TEXT,
         DefaultDataBufferFactory().wrap(ingestionMessageWithMissingValue().readBytes())))
      val session = MockWebSocketSession(uri = "/cask/json/OrderWindowSummary", input = sessionInput)
      wsHandler.handle(session).block()

      StepVerifier
         .create(session.textOutput.take(1))
         .expectNextMatches { json -> json.rejectedWithReason("Could not find json pointer /Symbol in record") }
         .verifyComplete()
   }

   @Test
   fun continueProcessingMessagesAfterError() {

      val malformedJson = WebSocketMessage(WebSocketMessage.Type.TEXT, DefaultDataBufferFactory().wrap(malformedJsonMessage().readBytes()))
      val invalidType = WebSocketMessage(WebSocketMessage.Type.TEXT, DefaultDataBufferFactory().wrap(invalidIngestionMessage().readBytes()))
      val validMessage = WebSocketMessage(WebSocketMessage.Type.TEXT, DefaultDataBufferFactory().wrap(validIngestionMessage().readBytes()))
      val sessionInput = Flux.just(malformedJson, invalidType, validMessage)
      val session = MockWebSocketSession(uri = "/cask/json/OrderWindowSummary?debug=true", input = sessionInput)
      wsHandler.handle(session).block()

      StepVerifier
         .create(session.textOutput.take(3))
         .expectNextMatches { json -> json.rejectedWithReason("com.fasterxml.jackson.core.io.JsonEOFException: Unexpected end-of-input in VALUE_STRING") }
         .expectNextMatches { json -> json.rejectedWithReason("Failed to parse value 6300USD to type Price - Failed to convert from type [java.lang.String] to type [java.math.BigDecimal] for value '6300USD'") }
         .expectNext("""{"result":"SUCCESS","message":"Successfully ingested 1 records"}""")
         .verifyComplete()
   }

   @Test
   fun csvMessageIngestion() {
      val validMessage = WebSocketMessage(WebSocketMessage.Type.TEXT, DefaultDataBufferFactory().wrap(validCsvMessage().readBytes()))
      val sessionInput = Flux.just(validMessage)
      val session = MockWebSocketSession(uri = "/cask/csv/OrderWindowSummaryCsv?debug=true", input = sessionInput)
      wsHandler.handle(session).block()

      StepVerifier
         .create(session.textOutput.take(1).timeout(Duration.ofSeconds(1)))
         .expectNext("""{"result":"SUCCESS","message":"Successfully ingested 1 records"}""")
         .verifyComplete()
   }

   @Test
   fun csvMessageIngestionWithSemicolonDelimiter() {
      val validMessage = WebSocketMessage(WebSocketMessage.Type.TEXT, DefaultDataBufferFactory().wrap(validCsvMessage(";").readBytes()))
      val sessionInput = Flux.just(validMessage)
      val session = MockWebSocketSession(uri = "/cask/csv/OrderWindowSummaryCsv?debug=true&delimiter=;", input = sessionInput)
      wsHandler.handle(session).block()

      StepVerifier
         .create(session.textOutput.take(1).timeout(Duration.ofSeconds(1)))
         .expectNext("""{"result":"SUCCESS","message":"Successfully ingested 1 records"}""")
         .verifyComplete()
   }

   @Test
   fun csvMessageIngestionWithSemicolonDelimiterUrlEncoded() {
      val validMessage = WebSocketMessage(WebSocketMessage.Type.TEXT, DefaultDataBufferFactory().wrap(validCsvMessage(";").readBytes()))
      val sessionInput = Flux.just(validMessage)
      val session = MockWebSocketSession(uri = "/cask/csv/OrderWindowSummaryCsv?debug=true&delimiter=%3B", input = sessionInput) // %3B = ;
      wsHandler.handle(session).block()

      StepVerifier
         .create(session.textOutput.take(1).timeout(Duration.ofSeconds(1)))
         .expectNext("""{"result":"SUCCESS","message":"Successfully ingested 1 records"}""")
         .verifyComplete()
   }

   @Test
   fun csvMessageIngestionWithoutColumnNameHeader() {
      val validMessage = WebSocketMessage(WebSocketMessage.Type.TEXT,
         DefaultDataBufferFactory().wrap(validCsvMessage(",", false).readBytes()))
      val sessionInput = Flux.just(validMessage)
      val session = MockWebSocketSession(uri = "/cask/csv/OrderWindowSummaryCsv?debug=true&firstRecordAsHeader=false", input = sessionInput)
      wsHandler.handle(session).block()

      StepVerifier
         .create(session.textOutput.take(1).timeout(Duration.ofSeconds(1)))
         .expectNext("""{"result":"SUCCESS","message":"Successfully ingested 1 records"}""")
         .verifyComplete()
   }

   @Test
   fun csvMessageIngestionError() {
      val validMessage = WebSocketMessage(WebSocketMessage.Type.TEXT, DefaultDataBufferFactory().wrap(invalidCsvMessage().readBytes()))
      val sessionInput = Flux.just(validMessage)
      val session = MockWebSocketSession(uri = "/cask/csv/OrderWindowSummaryCsv?debug=true", input = sessionInput)
      wsHandler.handle(session).block()

      StepVerifier
         .create(session.textOutput.take(1).timeout(Duration.ofSeconds(1)))
         .expectNext("""{"result":"SUCCESS","message":"Successfully ingested 0 records"}""")
         .verifyComplete()
   }

   @Test
   fun csvMessageIngestionWithNullValues() {
      val validMessage = WebSocketMessage(WebSocketMessage.Type.TEXT, DefaultDataBufferFactory().wrap(csvMessageWithNullValues().readBytes()))
      val sessionInput = Flux.just(validMessage)
      val session = MockWebSocketSession(uri = "/cask/csv/OrderWindowSummaryCsv?debug=true&nullValue=NULL&nullValue=UNKNOWN&nullValue=N%2FA", input = sessionInput) // N%2FA = N/A
      wsHandler.handle(session).block()

      StepVerifier
         .create(session.textOutput.take(1).timeout(Duration.ofSeconds(1)))
         .expectNext("""{"result":"SUCCESS","message":"Successfully ingested 4 records"}""")
         .verifyComplete()
   }

   @Test
   fun csvMessageIngestionWithHeaderOffset() {
      val validMessage = WebSocketMessage(WebSocketMessage.Type.TEXT, DefaultDataBufferFactory().wrap(csvMessageWithHeaderOffset().readBytes()))
      val sessionInput = Flux.just(validMessage)
      val session = MockWebSocketSession(uri = "/cask/csv/OrderWindowSummaryCsv?debug=true&nullValue=NULL&nullValue=UNKNOWN&nullValue=N%2FA&delimiter=%2C&firstRecordAsHeader=true&ignoreContentBefore=Date,Symbol", input = sessionInput) // N%2FA = N/A
      wsHandler.handle(session).block()

      StepVerifier
         .create(session.textOutput.take(1).timeout(Duration.ofSeconds(1)))
         .expectNext("""{"result":"SUCCESS","message":"Successfully ingested 2 records"}""")
         .verifyComplete()
   }

   private fun validCsvMessage(delimiter: String = ",", firstRecordAsHeader: Boolean = true): ByteArrayInputStream {
      val buf = StringBuilder()
      if (firstRecordAsHeader) {
         buf.append("Date,Symbol,Open,High,Low,Close\n")
      }
      buf.append("2020-03-19,BTCUSD,6300,6330,6186.08,6235.2")
      return buf.toString().replace(",", delimiter).byteInputStream()
   }

   private fun String.rejectedWithReason(reason:String):Boolean {
      val response = jacksonObjectMapper().readValue<CaskIngestionResponse>(this)
      response.result.should.equal(CaskIngestionResponse.ResponseResult.REJECTED)
      response.message.should.startWith(reason)
      return true
   }

   private fun csvMessageWithNullValues(): ByteArrayInputStream {
      return """
         Date,Symbol,Open,High,Low,Close
         NULL,BTCUSD,6300,6330,6186.08,6235.2
         2020-03-19,UNKNOWN,6300,6330,6186.08,6235.2
         2020-03-19,BTCUSD,N/A,6330,6186.08,6235.2
         2020-03-19,BTCUSD,6300,6330,6186.08,6235.2
      """.trimIndent().byteInputStream()
   }

   private fun csvMessageWithHeaderOffset(): ByteArrayInputStream {
      return """
         Before
         Header,,
         Date,Symbol,Open,High,Low,Close
         2020-03-19,BTCUSD,N/A,6330,6186.08,6235.2
         2020-03-19,BTCUSD,6300,6330,6186.08,6235.2
      """.trimIndent().byteInputStream()
   }

   private fun invalidCsvMessage(): ByteArrayInputStream {
      return """
         Date,Symbol,Open,High,Low,
         2020-03-19,BTCUSD,6300,6330,6186.08,6235.2
      """.trimIndent().byteInputStream()
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
