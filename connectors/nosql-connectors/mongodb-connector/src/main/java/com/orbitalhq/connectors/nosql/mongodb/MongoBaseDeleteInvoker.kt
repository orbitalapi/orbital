package com.orbitalhq.connectors.nosql.mongodb

import arrow.core.Either
import com.fasterxml.jackson.databind.ObjectMapper
import com.google.common.base.Stopwatch
import com.mongodb.client.result.DeleteResult
import com.orbitalhq.connectors.config.mongodb.MongoConnectionConfiguration
import com.orbitalhq.connectors.nosql.mongodb.MongoDeleteQueryInvoker.Companion
import com.orbitalhq.metrics.MetricTags
import com.orbitalhq.models.DataSourceUpdater
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.query.QueryContextEventDispatcher
import com.orbitalhq.query.StreamErrorMessage
import com.orbitalhq.query.tracing.DatabaseRequest
import com.orbitalhq.query.tracing.DatabaseResponse
import com.orbitalhq.query.tracing.OperationTraceSpan
import com.orbitalhq.query.tracing.SpanState
import com.orbitalhq.query.tracing.TraceEventDirection
import com.orbitalhq.query.tracing.TracingEventKind
import com.orbitalhq.schema.api.SchemaProvider
import com.orbitalhq.schemas.RemoteOperation
import com.orbitalhq.schemas.Schema
import com.orbitalhq.schemas.Service
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tag
import mu.KotlinLogging
import java.time.Duration

abstract class MongoBaseDeleteInvoker (
   connectionFactory: MongoConnectionFactory,
   schemaProvider: SchemaProvider,
   private val meterRegistry: MeterRegistry,
   private val objectMapper: ObjectMapper
) : MongoBaseInvoker(connectionFactory, schemaProvider, objectMapper) {

   companion object {
      private val logger = KotlinLogging.logger {}
   }
   protected fun tags(
      connectionConfig: MongoConnectionConfiguration,
      collectionName: String,
      operation: RemoteOperation
   ): List<Tag> {
      val tags = listOf(
         MetricTags.ConnectionName.of(connectionConfig.connectionName),
         MetricTags.TableName.of(collectionName),
         MetricTags.Operation.of(operation.name)
      )
      return tags
   }

   protected fun traceDeleteStart(
      traceContext: OperationTraceSpan,
      traceRequestMetadata: DatabaseRequest
   ) {
      traceContext.emitEvent(
         TracingEventKind.OK,
         SpanState.ACTIVE,
         null,
         traceRequestMetadata,
         "Delete",
         TraceEventDirection.OUTBOUND
      )
   }

   protected fun traceError(traceContext: OperationTraceSpan, error: Throwable) {
      traceContext.emitEvent(
         TracingEventKind.ERROR,
         SpanState.COMPLETE,
         null,
         DatabaseResponse(-1) { error.message },
         "Delete error",
         TraceEventDirection.INBOUND
      )
   }


   protected fun handleDeleteResult(
      deleteResult: DeleteResult,
      stopwatch: Stopwatch,
      connectionConfig: MongoConnectionConfiguration,
      collectionName: String,
      deleteCounter: Counter,
      traceContext: OperationTraceSpan,
      operation: RemoteOperation,
      service: Service,
      input: List<TypedInstance>,
      eventDispatcher: QueryContextEventDispatcher,
      queryId: String,
      schema: Schema
   ): Either<StreamErrorMessage, TypedInstance> {
      val deletedCount = deleteResult.deletedCount
      val elapsed = stopwatch.elapsed()

      logger.info { "Mongo delete call against collection ${connectionConfig.connectionName} / $collectionName completed in ${elapsed}ms, deleted $deletedCount records" }
      deleteCounter.increment(deletedCount.toDouble())

      traceContext.emitEvent(
         TracingEventKind.OK,
         SpanState.COMPLETE,
         operation.returnType,
         DatabaseResponse(deletedCount) { objectMapper.writeValueAsString(mapOf("deletedCount" to deletedCount)) },
         "Delete complete",
         TraceEventDirection.INBOUND
      )

      val operationResult = buildOperationResult(
         service,
         operation,
         input,
         "delete",
         connectionConfig.connectionString.hosts.joinToString(),
         elapsed,
         recordCount = deletedCount.toInt()
      )
      eventDispatcher.reportRemoteOperationInvoked(operationResult, queryId)

      return createDeleteResult(deletedCount, schema, service, operation, input, connectionConfig, elapsed)
   }


   protected fun createDeleteResult(
      deletedCount: Long,
      schema: Schema,
      service: Service,
      operation: RemoteOperation,
      input: List<TypedInstance>,
      connectionConfig: MongoConnectionConfiguration,
      elapsed: Duration
   ): Either<StreamErrorMessage, TypedInstance> {
      val resultData = deleteResult(deletedCount)

      val operationResult = buildOperationResult(
         service,
         operation,
         input,
         "delete",
         connectionConfig.connectionString.hosts.joinToString(),
         elapsed,
         recordCount = deletedCount.toInt()
      )

      return mapToTypedInstance(
         resultData,
         operation.returnType,
         schema,
         operationResult.asOperationReferenceDataSource()
      ).map { typedInstance ->
         DataSourceUpdater.update(typedInstance, operationResult.asOperationReferenceDataSource())
      }
   }
}
