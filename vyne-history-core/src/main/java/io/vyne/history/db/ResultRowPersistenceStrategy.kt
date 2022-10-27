package io.vyne.history.db

import com.fasterxml.jackson.databind.ObjectMapper
import io.vyne.history.HistoryPersistenceQueue
import io.vyne.history.QueryAnalyticsConfig
import io.vyne.models.DataSource
import io.vyne.models.FailedSearch
import io.vyne.models.OperationResult
import io.vyne.models.StaticDataSource
import io.vyne.models.TypeNamedInstanceMapper
import io.vyne.models.TypedInstanceConverter
import io.vyne.models.json.Jackson
import io.vyne.query.QueryResultEvent
import io.vyne.query.history.LineageRecord
import io.vyne.query.history.QueryResultRow
import io.vyne.query.history.RemoteCallResponse
import java.util.concurrent.ConcurrentHashMap


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

   fun resultRowPersistenceStrategy(objectMapper: ObjectMapper = Jackson.defaultObjectMapper,
                                    persistenceQueue: HistoryPersistenceQueue?,
                                    config: QueryAnalyticsConfig): ResultRowPersistenceStrategy {
      when {
         config.persistResults && persistenceQueue != null -> DatabaseResultRowPersistenceStrategy(objectMapper, persistenceQueue, config)
      }
      return if (config.persistResults) {
         DatabaseResultRowPersistenceStrategy(objectMapper, persistenceQueue, config)
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

class RemoteDatabaseResultRowPersistenceStrategy(private val objectMapper: ObjectMapper = Jackson.defaultObjectMapper,
                                                 private val config: QueryAnalyticsConfig)
   : DatabaseResultRowPersistenceStrategy(objectMapper, null, config) {
   override fun persistResultRowAndLineage(event: QueryResultEvent) {
      // noop
   }
}

open class DatabaseResultRowPersistenceStrategy(
   private val objectMapper: ObjectMapper = Jackson.defaultObjectMapper,
   private val persistenceQueue: HistoryPersistenceQueue?,
   private val config: QueryAnalyticsConfig) : ResultRowPersistenceStrategy {
   private val converter = TypedInstanceConverter(TypeNamedInstanceMapper)
   private val createdLineageRecordIds = ConcurrentHashMap<String, String>()

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
            (listOf(discoveredDataSource)).mapNotNull { dataSource ->
               // Some data sources (eg., streaming topics) actually span multiple queries.
               val scopedDataSourceId = "$queryId/${dataSource.id}"
               val previousLineageRecordId = createdLineageRecordIds.putIfAbsent(scopedDataSourceId, dataSource.id)
               val recordAlreadyPersisted = previousLineageRecordId != null

               if (recordAlreadyPersisted) null else LineageRecord(
                  dataSource.id,
                  queryId,
                  dataSource.name,
                  objectMapper.writeValueAsString(dataSource)
               )
            }

         }

      return lineageRecords
   }

   override fun createRemoteCallRecord(operationResult: OperationResult, queryId: String): RemoteCallResponse? {
      val responseJson = when (operationResult.remoteCall.response) {
         null -> "No response body received"
         // It's pretty rare to get a collection here, as the response value is the value before it's
         // been deserialzied.  However, belts 'n' braces.
         is Collection<*>, is Map<*, *> -> objectMapper.writeValueAsString(operationResult.remoteCall.response)
         else -> operationResult.remoteCall.response.toString()
      }
      return  RemoteCallResponse(
         responseId = operationResult.remoteCall.responseId,
         remoteCallId = operationResult.remoteCall.remoteCallId,
         queryId = queryId,
         response = responseJson
      )
   }

   override fun persistResultRowAndLineage(event: QueryResultEvent) {
      val resultRowCallsAndLineage = this.extractResultRowAndLineage(event)
      resultRowCallsAndLineage?.let {
         persistenceQueue?.storeResultRow(it.queryResultRow)
         it.remoteCalls.forEach { remoteCallResponse -> persistenceQueue?.storeRemoteCallResponse(remoteCallResponse) }
         it.lineageRecords.forEach { lineageRecord ->
            persistenceQueue?.storeLineageRecord(lineageRecord)
         }
      }
   }

   override fun extractResultRowAndLineage(event: QueryResultEvent): QueryResultRowLineage? {
      val (convertedTypedInstance, dataSources) = converter.convertAndCollectDataSources(event.typedInstance)
      val queryResultRow = QueryResultRow(
         queryId = event.queryId,
         json = objectMapper.writeValueAsString(convertedTypedInstance),
         valueHash = event.typedInstance.hashCodeWithDataSource
      )
      val remoteCalls = if (config.persistRemoteCallResponses) {
         dataSources
            .map { it.second }
            .filterIsInstance<OperationResult>()
            .distinctBy { it.remoteCall.responseId }
            .map { operationResult ->
               val responseJson = when (operationResult.remoteCall.response) {
                  null -> "No response body received"
                  // It's pretty rare to get a collection here, as the response value is the value before it's
                  // been deserialzied.  However, belts 'n' braces.
                  is Collection<*>, is Map<*, *> -> objectMapper.writeValueAsString(operationResult.remoteCall.response)
                  else -> operationResult.remoteCall.response.toString()
               }
               RemoteCallResponse(
                  responseId = operationResult.remoteCall.responseId,
                  remoteCallId = operationResult.remoteCall.remoteCallId,
                  queryId = event.queryId,
                  response = responseJson
               )
            }
      } else {
         listOf()
      }
      val lineageRecords = createLineageRecords(dataSources.map { it.second }, event.queryId)
      return QueryResultRowLineage(queryResultRow, remoteCalls, lineageRecords)

   }

}

data class QueryResultRowLineage(val queryResultRow: QueryResultRow, val remoteCalls: List<RemoteCallResponse>, val lineageRecords: List<LineageRecord>)

