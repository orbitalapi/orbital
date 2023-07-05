package io.vyne.history

import com.fasterxml.jackson.databind.ObjectMapper
import io.vyne.models.*
import io.vyne.models.json.Jackson
import io.vyne.query.QueryResultEvent
import io.vyne.query.history.LineageRecord
import io.vyne.query.history.QueryResultRow
import io.vyne.query.history.RemoteCallResponse
import mu.KotlinLogging
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.ExperimentalTime
import kotlin.time.measureTimedValue


interface ResultRowPersistenceStrategy {
   fun extractResultRowAndLineage(event: QueryResultEvent): QueryResultRowLineage? {
      // default no-op
      return null
   }

   fun persistResultRowAndLineage(event: QueryResultEvent) {
      // default no-op
   }

   fun createLineageRecords(
      dataSources: List<DataSource>,
      queryId: String
   ): List<LineageRecord>

   fun createRemoteCallRecord(operationResult: OperationResult, queryId: String): RemoteCallResponse?
}

object ResultRowPersistenceStrategyFactory {

   fun resultRowPersistenceStrategy(
      objectMapper: ObjectMapper = Jackson.defaultObjectMapper,
      persistenceQueue: HistoryPersistenceQueue?,
      config: QueryAnalyticsConfig
   ): ResultRowPersistenceStrategy {
      return if (config.persistRemoteCallResponses || config.persistRemoteCallMetadata || config.persistResults) {
         DatabaseResultRowPersistenceStrategy(
            objectMapper,
            persistenceQueue,
            config.persistRemoteCallResponses,
            config.persistRemoteCallMetadata,
            config.persistResults
         )
      } else {
         NoOpResultRowPersistenceStrategy()
      }

   }
}


class NoOpResultRowPersistenceStrategy : ResultRowPersistenceStrategy {
   override fun createLineageRecords(dataSources: List<DataSource>, queryId: String): List<LineageRecord> {
      return emptyList()
   }

   override fun createRemoteCallRecord(operationResult: OperationResult, queryId: String): RemoteCallResponse? {
      return null
   }
}


@OptIn(ExperimentalTime::class)
open class DatabaseResultRowPersistenceStrategy(
   private val objectMapper: ObjectMapper = Jackson.defaultObjectMapper,
   private val persistenceQueue: HistoryPersistenceQueue?,
   private val persistRemoteResponses: Boolean,
   private val persistRemoteMetadata: Boolean,
   private val persistResults: Boolean
) : ResultRowPersistenceStrategy {
   private val converter = TypedInstanceConverter(TypeNamedInstanceMapper)
   private val createdLineageRecordIds = ConcurrentHashMap<String, String>()

   companion object {
      private val logger = KotlinLogging.logger {}
   }

   override fun createLineageRecords(
      dataSources: List<DataSource>,
      queryId: String
   ): List<LineageRecord> {
      val lineageRecords = dataSources
         .filter { it !is StaticDataSource }
         .filter { it !is FailedSearch }
         .distinctBy { it.id }
         .flatMap { discoveredDataSource ->
            // Store the id of the lineage record we're creating in a hashmap.
            // If we get a value back, that means that the record has already been created,
            // so we don't need to persist it, and return null from this mapper

            //+ discoveredDataSource.failedAttempts
            (listOf(discoveredDataSource))
               .mapNotNull { dataSource ->
                  // Some data sources (eg., streaming topics) actually span multiple queries.
                  val scopedDataSourceId = "$queryId/${dataSource.id}"
                  val previousLineageRecordId = createdLineageRecordIds.putIfAbsent(scopedDataSourceId, dataSource.id)
                  val recordAlreadyPersisted = previousLineageRecordId != null

                  try {
                     if (recordAlreadyPersisted) null else LineageRecord(
                        dataSource.id,
                        queryId,
                        dataSource.name,
                        objectMapper.writeValueAsString(dataSource)
                     )
                  } catch (e: OutOfMemoryError) {
                     null
                  }
               }

         }

      return lineageRecords
   }

   override fun createRemoteCallRecord(operationResult: OperationResult, queryId: String): RemoteCallResponse? {
      return RemoteCallResponse.fromRemoteCall(
         operationResult.remoteCall,
         queryId,
         objectMapper,
         persistRemoteResponses
      )
   }

   override fun persistResultRowAndLineage(event: QueryResultEvent) {
      val resultRowCallsAndLineage = this.extractResultRowAndLineage(event)
      resultRowCallsAndLineage?.let {
         if (persistResults) {
            if (it.queryResultRow == null) {
               logger.warn { "persistResults is configured to true, but no results were emitted"}
            } else {
               persistenceQueue?.storeResultRow(it.queryResultRow)
            }
         }
         // Moved the persistence of remote calls into PersistingQueryEventConsumer, so that
         // we capture more calls - even those that fail
//         it.remoteCalls.forEach { remoteCallResponse -> persistenceQueue?.storeRemoteCallResponse(remoteCallResponse) }
         if (persistResults) {
            it.lineageRecords.forEach { lineageRecord ->
               persistenceQueue?.storeLineageRecord(lineageRecord)
            }
         }
      }
   }

   override fun extractResultRowAndLineage(event: QueryResultEvent): QueryResultRowLineage? {
      val result = measureTimedValue {
         val (convertedTypedInstance, dataSources) = converter.convertAndCollectDataSources(event.typedInstance)
         val queryResultRow = if (persistResults) QueryResultRow(
            queryId = event.queryId,
            json = objectMapper.writeValueAsString(convertedTypedInstance),
            valueHash = event.typedInstance.hashCodeWithDataSource
         ) else null
         val remoteCalls = if (persistRemoteResponses || persistRemoteMetadata) {
            dataSources
               .map { it.second }
               .filterIsInstance<OperationResult>()
               .distinctBy { it.remoteCall.responseId }
               .map { operationResult ->
                  RemoteCallResponse.fromRemoteCall(
                     operationResult.remoteCall,
                     event.queryId,
                     objectMapper,
                     persistRemoteResponses
                  )

               }
         } else {
            listOf()
         }
         val lineageRecords = createLineageRecords(dataSources.map { it.second }, event.queryId)
         QueryResultRowLineage(queryResultRow, remoteCalls, lineageRecords)
      }
      logger.debug { "extractResultRowAndLineage completed in ${result.duration}" }
      return result.value
   }

}

data class QueryResultRowLineage(
   // The result row - null if persisting results is disabled
   val queryResultRow: QueryResultRow?,
   val remoteCalls: List<RemoteCallResponse>,
   val lineageRecords: List<LineageRecord>
)

