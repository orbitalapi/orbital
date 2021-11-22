package io.vyne.history

import com.google.common.cache.CacheBuilder
import io.vyne.history.db.LineageSankeyViewBuilder
import io.vyne.history.db.QueryHistoryDao
import io.vyne.models.OperationResult
import io.vyne.models.TypedInstance
import io.vyne.query.QueryCompletedEvent
import io.vyne.query.QueryFailureEvent
import io.vyne.query.QueryResponse
import io.vyne.query.RestfulQueryExceptionEvent
import io.vyne.query.TaxiQlQueryExceptionEvent
import io.vyne.query.history.QuerySummary
import mu.KotlinLogging
import java.time.Instant

private val logger = KotlinLogging.logger {}
open class QuerySummaryPersister(private val queryHistoryDao: QueryHistoryDao) {
   protected var sankeyChartPersisted = false
   private val createdQuerySummaryIds = CacheBuilder.newBuilder()
      .build<String, String>()

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
       sankeyChartPersisted = true
   }

    fun persistEvent(event: RestfulQueryExceptionEvent) {
      createQuerySummaryRecord(event.queryId) {
         QueryResultEventMapper.toQuerySummary(event)
      }
      queryHistoryDao.setQueryEnded(event.queryId, event.timestamp, QueryResponse.ResponseStatus.ERROR, event.recordCount, event.message)

   }

    fun persistEvent(event: TaxiQlQueryExceptionEvent) {
      createQuerySummaryRecord(event.queryId) {
         QueryResultEventMapper.toQuerySummary(event)
      }
      queryHistoryDao.setQueryEnded(event.queryId, event.timestamp, QueryResponse.ResponseStatus.ERROR, event.recordCount, event.message)
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

   fun appendToSankeyChart(instance: TypedInstance, sankeyViewBuilder: LineageSankeyViewBuilder) {
      this.queryHistoryDao.appendToSankey(instance, sankeyViewBuilder)
   }

   fun appendOperationResultToSankeyChart(operation: OperationResult, sankeyViewBuilder: LineageSankeyViewBuilder) {
      this.queryHistoryDao.appendOperationResultToSankeyChart(operation, sankeyViewBuilder)
   }
}
