package com.orbitalhq.history.rest.export

import com.fasterxml.jackson.module.kotlin.readValue
import com.orbitalhq.history.api.RegressionPackRequest
import com.orbitalhq.models.TypeNamedInstance
import com.orbitalhq.models.json.Jackson
import com.orbitalhq.preflight.spec.ResultFormat
import com.orbitalhq.preflight.spec.Stub
import com.orbitalhq.preflight.spec.StubMode
import com.orbitalhq.preflight.spec.TestSpec
import com.orbitalhq.preflight.spec.TestSpecWriter
import com.orbitalhq.query.history.LineageRecord
import com.orbitalhq.query.history.QuerySummary
import com.orbitalhq.query.history.RemoteCallResponse
import com.orbitalhq.query.history.tracing.TraceEventRow
import com.orbitalhq.query.history.tracing.TraceSpanRecord
import com.orbitalhq.query.tracing.TraceEventDirection
import com.orbitalhq.query.tracing.TracingEventExchangeMetadata
import com.orbitalhq.schemas.OperationNames
import lang.taxi.types.Arrays
import mu.KotlinLogging
import org.springframework.stereotype.Component
import java.io.ByteArrayOutputStream

@Component
class PreflightSpecProvider {
   companion object {
      private val logger = KotlinLogging.logger {}
   }

   private val objectMapper = Jackson.defaultObjectMapper
   fun createTestSpec(
      resultsList: List<TypeNamedInstance>,
      summary: QuerySummary,
      lineage: List<LineageRecord>,
      calls: List<RemoteCallResponse>,
      spans: List<TraceSpanRecord>,
      request: RegressionPackRequest
   ): ByteArrayOutputStream {
      val response = if (Arrays.isArray(summary.responseType.orEmpty())) {
         resultsList.firstOrNull()?.convertToRaw()
      } else resultsList.map { it.convertToRaw() }
      val responseJson = objectMapper.writerWithDefaultPrettyPrinter()
         .writeValueAsString(response)

      val stubs = convertSpansToStubs(spans, calls)
      val testSpec = TestSpec(
         name = request.regressionPackName,
         description = request.description,
         query = summary.taxiQl ?: error("Request taxiQL was null"),
         resultFormat = ResultFormat.JSON,
         expectedResult = responseJson,
         dataSources = stubs,
         flow = null, // TOOD
         specVersion = "0.1" // TODO : Remove this when it's provided by TestSpec
      )
      val testSpecMarkdown = TestSpecWriter.write(testSpec)
      val outputStream = ByteArrayOutputStream()
      outputStream.write(testSpecMarkdown.toByteArray(Charsets.UTF_8))
      return outputStream
   }

   private fun convertSpansToStubs(spans: List<TraceSpanRecord>, calls: List<RemoteCallResponse>): List<Stub> {
      return spans.flatMap { it.flatten() }
         .filter { OperationNames.isName(it.eventSourceQualifiedName) }
         .map { span ->
            val events = span.events.mapNotNull { event ->
               val metadata = try {
                  // This is actually a TracingEventExchangeMetadata
                  // but it gets converted in ContextAwareEventMetadataMapper
                  // so that the payload is always persisted.
                  // However, deserialization back into the object isn't a design goal, so it doesn't always work
                  // They payload is really the only thing we care about when we go to build the stub
                  // so just read it back as a map
                  val exchangeMetadata = objectMapper.readValue<Map<String,Any>>(event.exchangeMetadata)
                  val payload = exchangeMetadata[TracingEventExchangeMetadata::payload.name] as String?
                  val eventType = exchangeMetadata["type"] as String?
                  if (eventType == null) {
                     logger.warn { "Failed to read exchange metadata for event ${event.eventId} with (${event.eventVerb} on ${event.eventResource}) - no event type was present - so cannot create stub"}
                     return@mapNotNull null
                  }
                  val payloadOrErrorMessage = if (payload.isNullOrEmpty() && event.direction == TraceEventDirection.INBOUND) {
                     logger.warn { "Event ${event.eventId} with (${event.eventVerb} on ${event.eventResource}) does not contain payload data"  }
                     """<!-- No payload data was captured for this event -->"""
                  } else {
                     payload.orEmpty()
                  }
                  val matchedCall: RemoteCallResponse? = if (event.remoteCallId != null) {
                     calls.firstOrNull { it.remoteCallId == event.remoteCallId }
                  } else null

                  ExchangeMetadataTypeAndPayload(eventType, payloadOrErrorMessage, matchedCall)
               } catch (e: Exception) {
                  logger.warn(e) { "Failed to read exchange metadata for event ${event.eventId} with (${event.eventVerb} on ${event.eventResource}), so cannot create stub" }
                  null
               } ?: return@mapNotNull null
               event to metadata
            }
            convertSpanToStub(span, events)

         }
   }

   private fun convertSpanToStub(
      span: TraceSpanRecord,
      events: List<Pair<TraceEventRow, ExchangeMetadataTypeAndPayload>>
   ): Stub {
      val (serviceName, operationName) = OperationNames.serviceAndOperation(span.eventSourceQualifiedName)
      val (mode, requestMessage, responseMessages) = convertEventsToMessages(events)
      val parameters = events.firstOrNull { it.first.direction == TraceEventDirection.OUTBOUND }
         ?.second?.remoteCall?.exchange?.parameters

      return Stub(
         operationName,
         span.eventSourceQualifiedName,
         mode,
         parameters =  parameters,
         response = if (mode == StubMode.REQUEST_RESPONSE) responseMessages.firstOrNull() else null,
         messages = if (mode == StubMode.STREAM) responseMessages else null,

      )
   }

   /**
    * Returns the stub mode, the request message, and the response message(s).
    *
    * The payload is available in the persisted JSON because [com.orbitalhq.history.db.ContextAwareEventMetadataMapper]
    * explicitly writes it when storing trace events, even though the property is @JsonIgnore by default.
    */
   private fun convertEventsToMessages(events: List<Pair<TraceEventRow, ExchangeMetadataTypeAndPayload>>): Triple<StubMode, String, List<String>> {
      var stubMode: StubMode = StubMode.REQUEST_RESPONSE
      var requestPayload = ""
      val responsePayloads = mutableListOf<String>()

      events.forEach { (eventRow, metadata) ->
         val payload = metadata.payload ?: ""
         when (metadata.eventType) {
            // HTTP
            "HttpRequest" -> requestPayload = payload ?: ""
            "HttpResponse" -> payload.let { responsePayloads.add(it) }

            // Database
            "DatabaseRequest" -> requestPayload = payload
            "DatabaseResponse" -> payload.let { responsePayloads.add(it) }
            "DatabaseResponseRecord" -> payload.let { responsePayloads.add(it) }
            "DatabaseResponseComplete" -> {} // no payload

            // Function calls
            "FunctionCallRequest" -> requestPayload = payload
            "FunctionCallResponse" -> payload.let { responsePayloads.add(it) }

            // Message streams (Kafka, Azure Service Bus, etc.)
            "MessageStreamSubscription" -> stubMode = StubMode.STREAM
            "MessageStreamEventReceived" -> {
               stubMode = StubMode.STREAM
               payload.let { responsePayloads.add(it) }
            }
            "MessageStreamDisconnection" -> stubMode = StubMode.STREAM
            "MessageStreamErrorEvent" -> {
               stubMode = StubMode.STREAM
               payload.let { responsePayloads.add(it) }
            }

            // Cache
            "CacheRequest" -> requestPayload = payload
            "CacheResponse" -> payload.let { responsePayloads.add(it) }

            // Object store
            "ObjectStoreRequest" -> requestPayload = payload
            "ObjectStoreResponse" -> payload.let { responsePayloads.add(it) }

            // No-op metadata
            "ConnectionError" -> {}
            "EmptyTraceMetadata" -> {}
            "ProjectionTraceMetadata" -> {}
         }
      }

      return Triple(stubMode, requestPayload, responsePayloads)
   }

}

private data class ExchangeMetadataTypeAndPayload(val eventType: String, val payload: String, val remoteCall: RemoteCallResponse?)
