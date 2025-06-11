package com.orbitalhq.history.db

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.common.base.Throwables
import com.orbitalhq.history.HistoryPersistenceQueue
import com.orbitalhq.history.QueryAnalyticsConfig
import com.orbitalhq.query.history.TraceEventRow
import com.orbitalhq.query.tracing.TracingEvent
import com.orbitalhq.query.tracing.TracingEventSink
import mu.KotlinLogging
import org.springframework.stereotype.Component
import java.time.ZoneId

@Component
class QueueWritingTraceEventSink(
   private val persistenceQueue: HistoryPersistenceQueue,
   private val objectMapper: ObjectMapper
) : TracingEventSink {
   override fun emitEvent(event: TracingEvent) {
      val eventRow = TraceEventRow(
         event.eventId,
         event.queryId,
         event.traceId,
         event.spanId,
         event.parentSpanId,
         event.tracingEventKind,
         event.spanState,
         event.timestamp.atZone(ZoneId.of("UTC")),
         objectMapper.writeValueAsString(event.exchangeMetadata)
      )
      persistenceQueue.storeTraceEvent(eventRow)
   }
}


@Component
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
