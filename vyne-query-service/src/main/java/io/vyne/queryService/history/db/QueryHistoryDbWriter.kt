package io.vyne.queryService.history.db

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.common.cache.CacheBuilder
import io.vyne.models.TypeNamedInstanceMapper
import io.vyne.models.TypedInstanceConverter
import io.vyne.models.json.Jackson
import io.vyne.query.QueryResponse
import io.vyne.queryService.history.QueryEvent
import io.vyne.queryService.history.QueryEventConsumer
import io.vyne.queryService.history.TaxiQlQueryResultEvent
import io.vyne.queryService.history.db.entity.*
import io.vyne.utils.log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.*
import javax.validation.ConstraintViolationException

@Component
class QueryHistoryDbWriter(
   private val repository: QueryHistoryRecordRepository,
   private val resultRowRepository: QueryResultRowRepository,
   private val lineageRecordRepository: LineageRecordRepository,
   private val objectMapper: ObjectMapper = Jackson.defaultObjectMapper
) : QueryEventConsumer {
   private val converter = TypedInstanceConverter(TypeNamedInstanceMapper)

   private val querySummaryCache = CacheBuilder.newBuilder()
      .build<String, PersistentQuerySummary>()


   override suspend fun handleEvent(event: QueryEvent) {
      when (event) {
         is TaxiQlQueryResultEvent -> persistEvent(event)
         else -> TODO("Event type ${event::class.simpleName} not yet supported")
      }
   }

   private suspend fun persistEvent(event: TaxiQlQueryResultEvent)  = withContext(Dispatchers.IO) {
      createQuerySummaryRecord(event.queryId) {
         PersistentQuerySummary(
            queryId = event.queryId,
            clientQueryId = event.clientQueryId ?: UUID.randomUUID().toString(),
            taxiQl = event.query,
            queryJson = null,
            startTime = Instant.now(),
            responseStatus = QueryResponse.ResponseStatus.INCOMPLETE
         )
      }
      resultRowRepository.save(
         QueryResultRow(
            queryId = event.queryId,
            json = objectMapper.writeValueAsString(converter.convert(event.typedInstance))
         )
      )
   }

   private fun createQuerySummaryRecord(queryId: String, factory: () -> PersistentQuerySummary) {
      // Since we don't have a "query started" concept (and it wouldn't
      // really work in a multi-threaded execution), we need to ensure that
      // the query object is present, as well as the result rows
      // Therefore, to avoid multiple trips to the db, we use a local
      // cache of created PersistentQuerySummary instances.
      // Note that this will fail when we allow execution across multiple JVM's.
      // At that point, we can simply wrap the insert in a try...catch, and let the
      // subsequent inserts fail.
      querySummaryCache.get(queryId) {
         val persistentQuerySummary = factory()
         try {
            log().info("Creating query history record for query $queryId")
            val fromDb = repository.save(
               persistentQuerySummary
            )
            log().info("Query history record for query $queryId created successfully")
            fromDb
         } catch (e: ConstraintViolationException) {
            log().info("Constraint violation thrown whilst persisting query history record for query $queryId, will not try to persist again.")
            persistentQuerySummary
         }

      }
   }
}

