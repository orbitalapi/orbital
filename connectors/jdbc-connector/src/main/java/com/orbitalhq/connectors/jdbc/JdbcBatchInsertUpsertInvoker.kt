package com.orbitalhq.connectors.jdbc

import arrow.core.Either
import com.fasterxml.jackson.databind.ObjectMapper
import com.orbitalhq.connectors.BatchWriteCacheProvider
import com.orbitalhq.models.DataSourceUpdater
import com.orbitalhq.models.OperationResultReference
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.query.QueryContextEventDispatcher
import com.orbitalhq.query.StreamErrorMessage
import com.orbitalhq.schema.api.SchemaProvider
import com.orbitalhq.schemas.Parameter
import com.orbitalhq.schemas.RemoteOperation
import com.orbitalhq.schemas.Service
import com.orbitalhq.schemas.fqn
import io.micrometer.core.instrument.MeterRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.job
import kotlinx.coroutines.reactive.asFlow
import lang.taxi.types.annotation
import mu.KotlinLogging
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger

class JdbcBatchInsertUpsertInvoker(
   connectionFactory: JdbcConnectionFactory,
   schemaProvider: SchemaProvider,
   private val objectMapper: ObjectMapper,
   private val meterRegistry: MeterRegistry,
   private val batchWriteCacheProvider: BatchWriteCacheProvider<TypedInstance, OperationResultReference>
): JdbcUpsertInvoker(connectionFactory, schemaProvider) {

   companion object {
      private val logger = KotlinLogging.logger {}
   }
   suspend fun invoke(
      service: Service,
      operation: RemoteOperation,
      parameters: List<Pair<Parameter, TypedInstance>>,
      eventDispatcher: QueryContextEventDispatcher,
      queryId: String,
      updateVerb: UpsertVerb,
      batchParams: BatchParams
   ): Flow<Either<StreamErrorMessage, TypedInstance>> {
      require(operation.parameters.size == 1) { "Operations annotated with ${JdbcConnectorTaxi.Annotations.UpsertOperationAnnotationName} should accept exactly one type" }
      val inputType = operation.parameters.single().type.let { type -> type.collectionType ?: type }
      require(inputType.hasMetadata(JdbcConnectorTaxi.Annotations.Table.NAME.fqn())) { "The input type into an ${JdbcConnectorTaxi.Annotations.UpsertOperationAnnotationName} operation should be a type with a ${JdbcConnectorTaxi.Annotations.Table.NAME.fqn()} annotation" }

      val (param, input) = parameters.singleOrNull()
         ?: error("Expected a single parameter, but received ${parameters.size}")
      val tableAnnotation =
         JdbcConnectorTaxi.Annotations.Table.from(inputType.taxiType.annotation(JdbcConnectorTaxi.Annotations.Table.NAME)!!)

      val (connectionConfig, jdbcTemplate) = getConnectionConfigAndTemplate(service)
      val dsl = sqlDsl(tableAnnotation.connectionName)

      createTableIfRequired(tableAnnotation, operation, dsl, jdbcTemplate, connectionConfig)

      val recordToWrite = parameters[0].second
      val tableName = SqlUtils.getTableName(recordToWrite.type.taxiType)
      val remoteCallId = UUID.randomUUID().toString()
      val traceContext = eventDispatcher.createOperationTraceSpan(service, operation, tableName, remoteCallId = remoteCallId)
      val batchWriteCache = batchWriteCacheProvider.forQueryId(
         queryId,
         batchParams.size,
         batchParams.durationMs,
         currentCoroutineContext().job
      ) { items ->
         logger.info { "Batch update triggered with ${items.size} items" }
         val (_,operationResult) = doInsertOrUpsert(
            schemaProvider.schema,
            connectionConfig,
            items,
            dsl,
            updateVerb,
            tableAnnotation,
            eventDispatcher,
            service,
            operation,
            parameters,
            queryId,
            inputType
         )
         Mono.just(operationResult)
      }
      return batchWriteCache.emit(recordToWrite)
         .map { operationResult ->
            Either.Right(DataSourceUpdater.update(recordToWrite, operationResult))
         }
         .asFlow()
         .flowOn(Dispatchers.IO)
   }

}
