package io.vyne.history

import com.google.common.cache.CacheBuilder
import io.vyne.history.db.LineageSankeyViewBuilder
import io.vyne.history.db.QueryHistoryDao
import io.vyne.models.OperationResult
import io.vyne.models.TypedInstance
import io.vyne.query.*
import io.vyne.query.history.QuerySummary
import io.vyne.schemas.Schema
import mu.KotlinLogging
import reactor.core.publisher.Sinks
import java.time.Duration
import java.time.Instant

private val logger = KotlinLogging.logger {}

open class QuerySummaryPersister(private val queryHistoryDao: QueryHistoryDao, private val queryId: String, private val schema:Schema) {

   private object Tick

   protected val sankeyViewBuilder = LineageSankeyViewBuilder(schema)

   private val createdQuerySummaryIds = CacheBuilder.newBuilder()
      .build<String, String>()

   private val throttledSankeyEventSink = Sinks.many().unicast().onBackpressureBuffer<Tick>()

   init {
      // Write sankey events to the db while the query is running.
      // To avoid being too db chatty, we throttle these events.
      throttledSankeyEventSink.asFlux()
         .bufferTimeout(50, Duration.ofSeconds(2))
         .subscribe {
            queryHistoryDao.persistSankeyChart(queryId, sankeyViewBuilder)
         }
   }


   protected fun createQuerySummaryRecord(queryId: String, factory: () -> QuerySummary) {
      // Since we don't have a "query started" concept (and it wouldn't
      // really work in a multi-threaded execution), we need to ensure that
      // the query object is present, as well as the result rows
      // Therefore, to avoid multiple trips to the db, we use a local
      // cache of created PersistentQuerySummary instances.
      // Note that this will fail when we allow execution across multiple JVM's.
      // At that point, we can simply wrap the insert in a try...catch, and let the
      // subsequent inserts fail.
      createdQuerySummaryIds.get(queryId) {
         val persistentQuerySummary = factory()
         try {
            queryHistoryDao.saveQuerySummary(
               persistentQuerySummary
            )
            queryId
         } catch (e: Exception) {
            logger.warn(e) { "Constraint violation thrown whilst persisting query history record for query $queryId, will not try to persist again." }
            queryId
         }

      }
   }

   fun persistEvent(event: QueryCompletedEvent, sankeyViewBuilder: LineageSankeyViewBuilder) {

      logger.info { "Recording that query ${event.queryId} has completed" }

      createQuerySummaryRecord(event.queryId) {
         QueryResultEventMapper.toQuerySummary(event)
      }

      queryHistoryDao.setQueryEnded(
         event.queryId,
         event.timestamp,
         QueryResponse.ResponseStatus.COMPLETED,
         event.recordCount
      )

      queryHistoryDao.persistSankeyChart(event.queryId, sankeyViewBuilder)
   }

   fun persistEvent(event: RestfulQueryExceptionEvent) {
      createQuerySummaryRecord(event.queryId) {
         QueryResultEventMapper.toQuerySummary(event)
      }
      queryHistoryDao.setQueryEnded(
         event.queryId,
         event.timestamp,
         QueryResponse.ResponseStatus.ERROR,
         event.recordCount,
         event.message
      )

   }

   fun persistEvent(event: TaxiQlQueryExceptionEvent) {
      createQuerySummaryRecord(event.queryId) {
         QueryResultEventMapper.toQuerySummary(event)
      }
      queryHistoryDao.setQueryEnded(
         event.queryId,
         event.timestamp,
         QueryResponse.ResponseStatus.ERROR,
         event.recordCount,
         event.message
      )
   }

   fun processStreamingQueryCancelledEvent(event: StreamingQueryCancelledEvent) {
      createQuerySummaryRecord(event.queryId) { QueryResultEventMapper.toQuerySummary(event) }
      queryHistoryDao.setQueryEnded(
         event.queryId,
         event.timestamp,
         QueryResponse.ResponseStatus.CANCELLED,
         event.recordCount,
         event.message
      )
   }

   fun persistEvent(event: QueryFailureEvent) {
      queryHistoryDao.setQueryEnded(
         event.queryId,
         Instant.now(),
         QueryResponse.ResponseStatus.ERROR,
         0,
         event.failure.message
      )
   }

   fun persistEvent(event: StreamingQueryCancelledEvent) {
      createQuerySummaryRecord(event.queryId) {
         QueryResultEventMapper.toQuerySummary(event)
      }
      queryHistoryDao.setQueryEnded(
         event.queryId,
         event.timestamp,
         QueryResponse.ResponseStatus.CANCELLED,
         event.recordCount,
         event.message
      )
   }

   fun appendToSankeyChart(instance: TypedInstance, sankeyViewBuilder: LineageSankeyViewBuilder) {
      this.queryHistoryDao.appendToSankey(instance, sankeyViewBuilder)
      queueSankeyPersistence()
   }

   private fun queueSankeyPersistence() {
      // Use tryEmitNext, as we're emitting from multiple threads
      val result = this.throttledSankeyEventSink.tryEmitNext(Tick)
      // We have multiple emitting threads.  Handle non-sequential access as described here:
      // https://stackoverflow.com/a/65202495
      when {
         result.isSuccess -> return
         result == Sinks.EmitResult.FAIL_NON_SERIALIZED -> return // Ignore these, as the next event will get it
         else -> {
            logger.warn { "Failed to emit sankey throttled persistence event: result = $result" }
         }

      }
   }

   fun appendOperationResultToSankeyChart(operation: OperationResult, sankeyViewBuilder: LineageSankeyViewBuilder) {
      this.queryHistoryDao.appendOperationResultToSankeyChart(operation, sankeyViewBuilder)
      queueSankeyPersistence()
   }
}
