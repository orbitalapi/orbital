package com.orbitalhq.query.history.tracing

import com.fasterxml.jackson.annotation.JsonRawValue
import com.orbitalhq.models.serde.ZonedDateTimeTimeSerializer
import com.orbitalhq.query.tracing.SpanState
import com.orbitalhq.query.tracing.TracingEventExchangeMetadata
import com.orbitalhq.query.tracing.TracingEventKind
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import kotlinx.serialization.Serializable
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.ZonedDateTime


/**
 * See TracingEvent for what all the properties mean
 */
@Entity(name = "TRACE_EVENT")
@Serializable
data class TraceEventRow(
   @Id
   @Column(name = "event_id")
   val eventId: String,

   @Column(name = "query_id")
   val queryId: String,

   @Column(name = "trace_id")
   val traceId: String,

   @Column(name = "span_id")
   val spanId: String,

   @Column(name = "parent_span_id")
   val parentSpanId: String?,

   @Column(name = "tracing_event_kind")
   @Enumerated(EnumType.STRING)
   val tracingEventKind: TracingEventKind,
   @Column(name = "span_state")
   @Enumerated(EnumType.STRING)
   val spanState: SpanState,
   @Serializable(with = ZonedDateTimeTimeSerializer::class)
   val timestamp: ZonedDateTime,

   @Column(name = "exchange_metadata", columnDefinition = "jsonb")
   @JdbcTypeCode(SqlTypes.JSON)
   @JsonRawValue
   val exchangeMetadata: String,

   /**
    * A verb, as determined by the event emitter, that provides a succinct description of
    * what this event was.
    * eg: "Subscribe", "Disconnect", "Receive", "Get", "Post", "Invoke", etc.
    */
   @Column(name = "event_verb")
   val eventVerb: String,
   /**
    * A human readable name for the resource that this event relates to.
    * Could be a table name, topic, url, etc.
    */
   @Column(name = "event_resource")
   val eventResource: String,
   /**
    * The qualified name of the operation, (or if this was a taxi function),
    * the taxi function qualified name
    */
   @Column(name = "event_source")
   val eventSourceQualifiedName: String,

   /**
    * The triggering event (if applicable).
    * Generally indicates that this event (normally the start of a span)
    * was triggered by associated event
    */
   @Column(name = "linked_event_id")
   val linkedEventId: String?
)
