package com.orbitalhq.history.db

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.convertValue
import com.google.common.base.Throwables
import com.orbitalhq.history.HistoryPersistenceQueue
import com.orbitalhq.history.QueryAnalyticsConfig
import com.orbitalhq.history.db.tracing.TraceEventRepository
import com.orbitalhq.query.history.tracing.TraceEventRow
import com.orbitalhq.query.tracing.SpanEventSource
import com.orbitalhq.query.tracing.TracingEvent
import com.orbitalhq.query.tracing.TracingEventExchangeMetadata
import com.orbitalhq.query.tracing.TracingEventSink
import com.orbitalhq.schemas.QueryOptions
import com.orbitalhq.schemas.Schema
import mu.KotlinLogging
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import java.time.ZoneId


/**
 * Converts the TracingEventExchangeMetadata into JSON.
 * Allows for fine-grained control over how payload bodies etc are converted
 * by allowing annotations on queries, operations, services, and payload types, which
 * can override the default behaviour
 */
class ContextAwareEventMetadataMapper(
   private val queryOptions: QueryOptions,
   private val objectMapper: ObjectMapper,
   private val analyticsConfig: QueryAnalyticsConfig,
   private val schema: Schema
) {
   fun eventMetadataToJson(eventExchangeMetadata: TracingEventExchangeMetadata, source: SpanEventSource): String {
      val metadataAsMap = objectMapper.convertValue<Map<String, Any?>>(eventExchangeMetadata)
         .toMutableMap()
      // TODO : Apply filtering.
      // For now, we always write the entire payload.
      val payload = eventExchangeMetadata.payload()
      metadataAsMap[TracingEventExchangeMetadata::payload.name] = payload
      val json = objectMapper.writeValueAsString(metadataAsMap)
      return json
   }
}

class QueueWritingTraceEventSink(
   private val persistenceQueue: HistoryPersistenceQueue,
   private val metadataMapper: ContextAwareEventMetadataMapper
) : TracingEventSink {
   override fun emitEvent(source: SpanEventSource, event: TracingEvent) {
      // TODO: Need to do some filtering of payloads and events based on metadata here.
      val eventRow = TraceEventRow(
         event.eventId,
         event.queryId,
         event.traceId,
         event.spanId,
         event.parentSpanId,
         event.tracingEventKind,
         event.spanState,
         event.timestamp.atZone(ZoneId.of("UTC")),
         metadataMapper.eventMetadataToJson(event.exchangeMetadata, source),
         event.eventVerb,
         event.eventResource,
         event.eventSourceQualifiedName,
         event.linkedEventId
      )
      persistenceQueue.storeTraceEvent(eventRow)
   }
}


@Component
@ConditionalOnProperty(name = ["vyne.db.enabled"], havingValue = "true", matchIfMissing = true)
class PersistingTraceEventConsumer(
   private val persistenceQueue: HistoryPersistenceQueue,
   private val traceEventRepository: TraceEventRepository,
   private val config: QueryAnalyticsConfig,
) {
   companion object {
      private val logger = KotlinLogging.logger {}
   }

   init {
      persistenceQueue.retrieveTraceEvents().index()
         .publishOn(QuerySummaryPersister.queryHistoryScheduler)
         .bufferTimeout(config.writerMaxBatchSize, config.writerMaxDuration)
         .doOnError { error ->
            val rootCause = Throwables.getRootCause(error)
            logger.error(rootCause) { "Subscription to TraceEvents queue for all queries encountered an error ${rootCause.message}" }
         }
         .doOnComplete { logger.info { "Subscription to TraceEvents queue for All Queries has completed" } }
         .subscribe { batch ->
            if (!config.persistTraceEvents) {
               return@subscribe
            }
            try {
               traceEventRepository.saveAll(batch.map { it.t2 })
               logger.debug { "Processing TraceEvents on Queue for All Queries - position ${batch.lastOrNull()?.t1}" }
            } catch (e: Exception) {
               val rootCause = Throwables.getRootCause(e)
               logger.warn(rootCause) { "Persisting batch of TraceEvents failed: ${rootCause.message}" }
            }
         }
   }
}
