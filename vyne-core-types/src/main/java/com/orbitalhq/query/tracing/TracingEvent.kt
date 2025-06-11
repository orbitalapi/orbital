package com.orbitalhq.query.tracing

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.orbitalhq.query.CacheExchange
import com.orbitalhq.utils.Ids
import kotlinx.serialization.Serializable
import java.time.Instant




/**
 * A tracing event.
 *
 * We typically split request / response into two events. This is because:
 *  - Request/Response can be asymmetric (one request, multiple responses) in things like Brokers
 *  - We don't know the response outcome at the time the request is sent, and we don't want to wait.
 *
 * Try to emit Request events at the time the request was sent, and
 * response events later, when the response is received.
 */
data class TracingEvent(
   /**
    * The query Id. Do not use clientQueryId here
    */
   val queryId: String,
   /**
    * TraceId. Never null.
    * If not provided upstream, then create one.
    * A trace is the top-level, longest-lived concept in tracing, and encapsulates all the
    * activities in a full operation.
    *
    * Companies using OTEL or e2e tracing will supply this - sometimes on an HTTP header. If not, and you're the first
    * event in the chain, create a new tracing id
    */
   val traceId: String,
   /**
    * Represents this specific activity.
    * A request / response would share the same spanId.
    *
    * (Http call request/response, Kafka subscription, etc.)
    * Multiple events can relate to a single Span
    * For things that are long-lived, (like Kafka), you'd have a single span for the entire subscription,
    * with multiple tracing events.
    */
   val spanId: String,

   /**
    * The span that spawned this span. Only null if this is the root event.
    * When capturing a response to a request, the parentSpanId is the same as the parentSpanId on the request event.
    */
   val parentSpanId: String?,
   val tracingEventKind: TracingEventKind,
   val spanState: SpanState,
   val exchangeMetadata: TracingEventExchangeMetadata,
   /**
    * The id for this specific event.
    * Unlike traceId and spanId, this id is entirely unique, and represents just this event.
    */
   val eventId: String = Ids.fastUuid(),
   val timestamp: Instant = Instant.now(),

   ) {
   companion object {
      fun newTraceId(): String {
         return Ids.fastUuid()
      }

      fun newSpanId(): String {
         return Ids.fastUuid()
      }
   }
}

/**
 * Indicates the state of the span at the point of this event.
 * A
 */
enum class SpanState {
   ACTIVE,
   COMPLETE
}

enum class TracingEventKind {
   /**
    * Indicates whether this specific event is tracking a successful interaction.
    * Does not track the interaction of the full exchange.
    * For example, a request event has an OK state if it was delivered to the destination, regardless of whether the
    * full exchange was succesful.
    */
   OK,

   /**
    * Indicates that the event models some form of failure or error.
    * A request would fail if we were unable to deliver it (bad DNS, etc).
    * A response would fail if the server returned a failure (4xx, 5xx, etc)
    */
   ERROR
}

@Serializable
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(
   JsonSubTypes.Type(value = HttpRequest::class, name = "HttpRequest"),
   JsonSubTypes.Type(value = HttpResponse::class, name = "HttpResponse"),
   JsonSubTypes.Type(value = SqlRequest::class, name = "SqlRequest"),
   JsonSubTypes.Type(value = SqlResponse::class, name = "SqlResponse"),
   JsonSubTypes.Type(value = MessageStreamSubscription::class, name = "MessageStreamSubscription"),
   JsonSubTypes.Type(value = MessageStreamDisconnection::class, name = "MessageStreamDisconnection"),
   JsonSubTypes.Type(value = MessageStreamEventReceived::class, name = "MessageStreamEventReceived"),
   JsonSubTypes.Type(value = CacheRequest::class, name = "CacheRequest"),
   JsonSubTypes.Type(value = CacheResponse::class, name = "CacheResponse"),
   JsonSubTypes.Type(value = ObjectStoreRequest::class, name = "ObjectStoreRequest"),
   JsonSubTypes.Type(value = ObjectStoreResponse::class, name = "ObjectStoreResponse"),
)
sealed class TracingEventExchangeMetadata {
   /**
    * The actual, raw payload sent / received.
    * This should be as raw as possible, ideally a string containing
    * the actual content, before it's been parsed / validated.
    * However, it's acceptable to apply redaction to remove sensitive data - the actual
    * implementation of this is left to the emitter.
    *
    * This payload may be null if the event system has disabled capture for the source,
    * and it may be truncated if the payload size exceeds configured defaults.
    */
   abstract val payload: String?
}

@Serializable
data class HttpRequest(
   val url: String,
   val verb: String,
   override val payload: String?,
   /**
    * The size in bytes
    */
   val size: Long,
   val headers: Map<String, List<String>>
) : TracingEventExchangeMetadata() {
}

@Serializable
data class HttpResponse(
   val responseCode: Int,
   override val payload: String?,
   /**
    * The size in bytes
    */
   val size: Long,
   val headers: Map<String, List<String>>
) : TracingEventExchangeMetadata()

@Serializable
data class SqlRequest(
   override val payload: String?,
   val connectionName: String
) : TracingEventExchangeMetadata()

@Serializable
data class SqlResponse(
   override val payload: String?,
   val recordCount: Int
) : TracingEventExchangeMetadata()

@Serializable
data class MessageStreamSubscription(
   val address: String,
   val topic: String,
   val connectionName: String
) : TracingEventExchangeMetadata() {
   override val payload: String? = null
}

@Serializable
data object MessageStreamDisconnection : TracingEventExchangeMetadata() {
   override val payload: String? = null
}

@Serializable
data class MessageStreamEventReceived(
   override val payload: String?,
   /**
    * The size in bytes
    */
   val size: Long,
) : TracingEventExchangeMetadata()

@Serializable
data class CacheRequest(
   val cacheName: String,
   val verb: CacheExchange.CacheOperationVerb,
   val connectionName: String
) : TracingEventExchangeMetadata() {
   /**
    * If performing a key-based lookup, this is the key.
    * If doing a cache query, this is the actual query
    */
   override val payload: String? = null
}

@Serializable
data class CacheResponse(
   val recordCount: Int,
   override val payload: String?,
) : TracingEventExchangeMetadata()

@Serializable
data class ObjectStoreRequest(
   val connectionName: String,
   val bucketName: String
) : TracingEventExchangeMetadata() {
   /**
    * If a SQL query is being performed (and the object store supports it),
    * use the sql query here.
    * Otherwise, if this is a file-based interaction, use the file name (or file pattern)
    */
   override val payload: String? = null
}

@Serializable
data class ObjectStoreResponse(
   override val payload: String?,
   /**
    * The size in bytes
    */
   val size: Long,
) : TracingEventExchangeMetadata()
