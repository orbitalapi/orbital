package com.orbitalhq.query.tracing

import com.fasterxml.jackson.annotation.JsonIgnore
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
   val direction: TraceEventDirection,
   val spanState: SpanState,
   val exchangeMetadata: TracingEventExchangeMetadata,
   /**
    * A verb, as determined by the event emitter, that provides a succinct description of
    * what this event was.
    * eg: "Subscribe", "Disconnect", "Receive", "Get", "Post", "Invoke", etc.
    */
   val eventVerb: String,
   /**
    * A human readable name for the resource that this event relates to.
    * Could be a table name, topic, url, etc.
    */
   val eventResource: String,
   /**
    * The qualified name of the operation, (or if this was a taxi function),
    * the taxi function qualified name
    */
   val eventSourceQualifiedName: String,

   /**
    * Indicates this event is linked to another event.
    * Generally, it indicates that this event was triggered by the other
    * event
    */
   val linkedEventId: String?,
   /**
    * The id for this specific event.
    * Unlike traceId and spanId, this id is entirely unique, and represents just this event.
    */
   val eventId: String = Ids.fastUuid(),
   val timestamp: Instant = Instant.now(),

   /**
    * The associated remote call, if one exists.
    * Not all trace events have a remote call.
    */
   val remoteCallId: String? = null

   ) {
   val idSet = TracingEventIdSet(eventId, spanId, traceId)

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
 * The collection of IDs used to help identify a single specific event
 */
data class TracingEventIdSet(
   /**
    * This is the unique event id.
    * This is really all you need
    */
   val eventId: String,
   // These are for reference / to help
   val spanId: String,
   val traceId: String
)

enum class TraceEventDirection {
   /**
    * Something received by Orbital - a triggering http request, a response to a request we sent, a kafka message, etc.
    */
   INBOUND,

   /**
    * Something we're sending out
    */
   OUTBOUND,

   /**
    * Use for things that don't have direction, such as internal logging events
    */
   NONE
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
   JsonSubTypes.Type(value = DatabaseRequest::class, name = "DatabaseRequest"),
   JsonSubTypes.Type(value = DatabaseResponse::class, name = "DatabaseResponse"),
   JsonSubTypes.Type(value = DatabaseResponseRecord::class, name = "DatabaseResponseRecord"),
   JsonSubTypes.Type(value = MessageStreamSubscription::class, name = "MessageStreamSubscription"),
   JsonSubTypes.Type(value = MessageStreamDisconnection::class, name = "MessageStreamDisconnection"),
   JsonSubTypes.Type(value = MessageStreamErrorEvent::class, name = "MessageStreamErrorEvent"),
   JsonSubTypes.Type(value = MessageStreamEventReceived::class, name = "MessageStreamEventReceived"),
   JsonSubTypes.Type(value = CacheRequest::class, name = "CacheRequest"),
   JsonSubTypes.Type(value = CacheResponse::class, name = "CacheResponse"),
   JsonSubTypes.Type(value = ObjectStoreRequest::class, name = "ObjectStoreRequest"),
   JsonSubTypes.Type(value = ObjectStoreResponse::class, name = "ObjectStoreResponse"),
   JsonSubTypes.Type(value = ProjectionTraceMetadata::class, name = "ProjectionTraceMetadata"),
   JsonSubTypes.Type(value = EmptyTraceMetadata::class, name = "EmptyTraceMetadata"),
   JsonSubTypes.Type(value = FunctionCallRequest::class, name = "FunctionCallRequest"),
   JsonSubTypes.Type(value = FunctionCallResponse::class, name = "FunctionCallResponse"),
   JsonSubTypes.Type(value = ConnectionError::class, name = "ConnectionError"),
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
    *
    * Note: Although this is @JsonIgnore for default serialization, the payload is explicitly
    * written into the persisted JSON by [com.orbitalhq.history.db.ContextAwareEventMetadataMapper]
    * when storing to the database. This means the payload IS available when deserializing
    * from stored trace events.
    */
   @get:JsonIgnore
   abstract val payload: suspend () -> String?
}

@Serializable
data class FunctionCallRequest(
   val functionName: String,
   override val payload: suspend () -> String?,
): TracingEventExchangeMetadata()

@Serializable
data class FunctionCallResponse(
   val size: Long,
   val error: String? = null,
   val statusCode: Int,
   override val payload: suspend () -> String?,
): TracingEventExchangeMetadata()

@Serializable
data class HttpRequest(
   val url: String,
   val verb: String,
   override val payload: suspend () -> String?,
   /**
    * The size in bytes
    */
   val size: Long,
   val headers: Map<String, List<String>>
) : TracingEventExchangeMetadata()


/**
 * Emitted when a service (any kind of service) failed to connect
 */
@Serializable
data class ConnectionError(
   val message: String,
) : TracingEventExchangeMetadata() {
   override val payload: suspend () -> String? = { null }
}

data object EmptyTraceMetadata : TracingEventExchangeMetadata() {
   override val payload: suspend () -> String? = { null }
}

data class ProjectionTraceMetadata(
   override val payload: suspend () -> String?,
) : TracingEventExchangeMetadata()

@Serializable
data class HttpResponse(
   val responseCode: Int,
   override val payload: suspend () -> String?,
   /**
    * The size in bytes
    */
   val size: Long,
   val headers: Map<String, List<String>>
) : TracingEventExchangeMetadata()

@Serializable
data class DatabaseRequest(
   val connectionName: String,
   val verb: String,
   val tableName: String,
   override val payload: suspend () -> String?,
) : TracingEventExchangeMetadata()

/**
 * Emitted when working with reactive database sources,
 * where each record is emitted individually, without a total count
 */
@Serializable
data class DatabaseResponseRecord(
   override val payload: suspend () -> String?,
) : TracingEventExchangeMetadata()

/**
 * Emitted when working with reactive database sources,
 * after all responses have been returned
 */
@Serializable
data class DatabaseResponseComplete(
   val recordCount: Int,
) : TracingEventExchangeMetadata() {
   override val payload: suspend () -> String? = { null }
}

/**
 * Emitted when working with database responses that are non-streaming,
 * where the full result set (if any) is returned in a single payload.
 */
@Serializable
data class DatabaseResponse(
   val recordCount: Long,
   override val payload: suspend () -> String?,
) : TracingEventExchangeMetadata()

@Serializable
data class MessageStreamSubscription(
   val topic: String,
   val connectionName: String,
   val subscriptionAction: SubscriptionAction
) : TracingEventExchangeMetadata() {
   enum class SubscriptionAction {
      NOT_CAPTURED,
      JOINED_EXISTING_SUBSCRIPTION,
      CREATED_NEW_SUBSCRIPTION
   }

   override val payload: suspend () -> String? = { null }
}

@Serializable
data class MessageStreamDisconnection(
   val disconnectionAction: DisconnectionAction
) : TracingEventExchangeMetadata() {
   override val payload: suspend () -> String? = { null }

   enum class DisconnectionAction {
      NOT_CAPTURED,
      SUBSCRIPTION_TERMINATED,
      SUBSCRIPTION_LEFT_ACTIVE
   }
}

@Serializable
data class MessageStreamEventReceived(
   /**
    * The size in bytes
    */
   val size: Long,
   val payloadEncoding: PayloadEncoding,
   override val payload: suspend () -> String?,
) : TracingEventExchangeMetadata()

enum class PayloadEncoding {
   STRING,
   BASE64_BYTEARRAY
}

@Serializable
data class MessageStreamErrorEvent(
   /**
    * The size in bytes
    */
   val size: Long,
   val errorMessage: String,
   val payloadEncoding: PayloadEncoding,
   override val payload: suspend () -> String?,
) : TracingEventExchangeMetadata()

@Serializable
data class CacheRequest(
   val cacheName: String,
   val verb: CacheExchange.CacheOperationVerb,
   val connectionName: String,
   /**
    * If performing a key-based lookup, this is the key.
    * If doing a cache query, this is the actual query
    */
   override val payload: suspend () -> String? = { null }
) : TracingEventExchangeMetadata() {

}

@Serializable
data class CacheResponse(
   val recordCount: Int,
   override val payload: suspend () -> String?,
) : TracingEventExchangeMetadata()

@Serializable
data class ObjectStoreRequest(
   val connectionName: String,
   val bucketName: String,
   /**
    * If a SQL query is being performed (and the object store supports it),
    * use the sql query here.
    * Otherwise, if this is a file-based interaction, use the file name (or file pattern)
    */
   override val payload: suspend () -> String? = { null }
) : TracingEventExchangeMetadata() {

}

@Serializable
data class ObjectStoreResponse(
   /**
    * The size in bytes
    */
   val errorMessage: String? = null,
   val size: Long,
   override val payload: suspend () -> String?,
) : TracingEventExchangeMetadata()
