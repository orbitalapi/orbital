package io.vyne.history.db

import arrow.core.extensions.list.functorFilter.filter
import com.fasterxml.jackson.databind.ObjectMapper
import io.vyne.models.DataSource
import io.vyne.models.FailedSearch
import io.vyne.models.OperationResult
import io.vyne.models.StaticDataSource
import io.vyne.models.TypeNamedInstanceMapper
import io.vyne.models.TypedInstanceConverter
import io.vyne.models.json.Jackson
import io.vyne.query.history.LineageRecord
import io.vyne.query.history.QueryResultRow
import io.vyne.query.history.RemoteCallResponse
import io.vyne.history.HistoryPersistenceQueue
import io.vyne.history.QueryHistoryConfig
import io.vyne.query.QueryResultEvent
import mu.KotlinLogging
import java.util.concurrent.ConcurrentHashMap


interface ResultRowPersistenceStrategy {
   fun persistResultRowAndLineage(event: QueryResultEvent) {
      // default no-op
   }
   fun createLineageRecords(
      dataSources: List<DataSource>,
      queryId: String
   ): List<LineageRecord>
}

object ResultRowPersistenceStrategyFactory {
   fun ResultRowPersistenceStrategy(objectMapper: ObjectMapper = Jackson.defaultObjectMapper,
                                    persistenceQueue: HistoryPersistenceQueue,
                                    config: QueryHistoryConfig): ResultRowPersistenceStrategy {
      return if (config.persistResults) {
         DatabaseResultRowPersistenceStrategy(objectMapper, persistenceQueue, config)
      } else {
         NoOpResultRowPersistenceStrategy()
      }

   }
}


class NoOpResultRowPersistenceStrategy: ResultRowPersistenceStrategy {
   override fun createLineageRecords(dataSources: List<DataSource>, queryId: String): List<LineageRecord> {
      return emptyList()
   }
}
private val logger = KotlinLogging.logger {}
class DatabaseResultRowPersistenceStrategy(
   private val objectMapper: ObjectMapper = Jackson.defaultObjectMapper,
   private val persistenceQueue: HistoryPersistenceQueue,
   private val config: QueryHistoryConfig): ResultRowPersistenceStrategy {
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
               val previousLineageRecordId = createdLineageRecordIds.putIfAbsent(dataSource.id, dataSource.id)
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

   override fun persistResultRowAndLineage(event: QueryResultEvent) {

      val (convertedTypedInstance, dataSources) = converter.convertAndCollectDataSources(event.typedInstance)

      persistenceQueue.storeResultRow(
         QueryResultRow(
            queryId = event.queryId,
            json = objectMapper.writeValueAsString(convertedTypedInstance),
            valueHash = event.typedInstance.hashCodeWithDataSource
         )
      )

      if (config.persistRemoteCallResponses) {
         dataSources
            .map { it.second }
            .filterIsInstance<OperationResult>()
            .distinctBy { it.remoteCall.responseId }
            .forEach { operationResult ->
               val responseJson = when (operationResult.remoteCall.response) {
                  null -> "No response body received"
                  // It's pretty rare to get a collection here, as the response value is the value before it's
                  // been deserialzied.  However, belts 'n' braces.
                  is Collection<*>, is Map<*, *> -> objectMapper.writeValueAsString(operationResult.remoteCall.response)
                  else -> operationResult.remoteCall.response.toString()
               }
               val remoteCallRecord = RemoteCallResponse(
                  responseId = operationResult.remoteCall.responseId,
                  remoteCallId = operationResult.remoteCall.remoteCallId,
                  queryId = event.queryId,
                  response = responseJson
               )
               try {
                  persistenceQueue.storeRemoteCallResponse(remoteCallRecord)
               } catch (exception: Exception) {
                  logger.warn(exception) { "Unable to add RemoteCallResponse to the persistence queue" }
               }
            }
      }

      val lineageRecords = createLineageRecords(dataSources.map { it.second }, event.queryId)
      lineageRecords.forEach {
         persistenceQueue.storeLineageRecord(it)
      }
   }
}

