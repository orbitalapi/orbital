package com.orbitalhq.connectors.nosql.mongodb

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.left
import com.fasterxml.jackson.databind.ObjectMapper
import com.google.common.base.Stopwatch
import com.orbitalhq.connectors.metrics.captureMetrics
import com.orbitalhq.metrics.MetricTags
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.query.QueryContextEventDispatcher
import com.orbitalhq.query.StreamErrorMessage
import com.orbitalhq.query.tracing.DatabaseRequest
import com.orbitalhq.query.tracing.DatabaseResponse
import com.orbitalhq.query.tracing.SpanState
import com.orbitalhq.query.tracing.TraceEventDirection
import com.orbitalhq.query.tracing.TracingEventKind
import com.orbitalhq.schema.api.SchemaProvider
import com.orbitalhq.schemas.Parameter
import com.orbitalhq.schemas.QueryOptions
import com.orbitalhq.schemas.RemoteOperation
import com.orbitalhq.schemas.Service
import io.micrometer.core.instrument.MeterRegistry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import mu.KotlinLogging
import java.time.Duration

class MongoNativeAggregateQueryInvoker(
   connectionFactory: MongoConnectionFactory,
   schemaProvider: SchemaProvider,
   private val meterRegistry: MeterRegistry,
   private val objectMapper: ObjectMapper
) : MongoBaseInvoker(connectionFactory, schemaProvider, objectMapper) {
   companion object {
      private val logger = KotlinLogging.logger {}
   }

   private val aggregateBuilder = MongoAggregateBuilder(objectMapper)
   fun invoke(
      service: Service,
      operation: RemoteOperation,
      parameters: List<Pair<Parameter, TypedInstance>>,
      eventDispatcher: QueryContextEventDispatcher,
      queryId: String,
      queryOptions: QueryOptions
   ): Flow<Either<StreamErrorMessage, TypedInstance>> {
      val (mongoConnectionConfig, reactiveMongoTemplate) = getConnectionConfigAndTemplate(service)
      val schema = schemaProvider.schema
      val taxiSchema = schema.taxi
      val tags = listOf(
         MetricTags.ConnectionName.of(mongoConnectionConfig.connectionName),
         MetricTags.Operation.of(operation.name)
      )
      val aggregateAnnotation =
         MongoConnector.Annotations.MongoAggregate.from(operation.firstMetadata(MongoConnector.Annotations.MongoAggregateName.parameterizedName))
      val collectionName = aggregateAnnotation.collection
      val aggregateParams = extractAggregationParameters(parameters, operation)
      val (aggregation, pipelineDocuments) = aggregateBuilder.buildAggregation(aggregateAnnotation.pipeline, aggregateParams)
         .getOrElse {  exception ->
            val errorMessage = StreamErrorMessage.fromException(exception, MongoConnector.Annotations.MongoAggregateName.parameterizedName)
            return flowOf(errorMessage.left())
         }

      val traceSpan = eventDispatcher.createOperationTraceSpan(service, operation, collectionName)
      val stopwatch = Stopwatch.createStarted()
      val aggregateJson = aggregation.toString()

      logger.info { "Executing Mongo Aggregate: $aggregateJson" }

      val resultFlux = reactiveMongoTemplate.aggregate(aggregation, collectionName, Map::class.java)
         .captureMetrics(tags, meterRegistry)
         .doOnSubscribe {
            traceSpan.emitEvent(
               TracingEventKind.OK,
               SpanState.ACTIVE,
               null,
               DatabaseRequest(mongoConnectionConfig.connectionName, "find", collectionName) { aggregateJson },
               "Aggregate",
               TraceEventDirection.OUTBOUND)
         }
         .onErrorMap { error ->
            traceSpan.emitEvent(
               TracingEventKind.ERROR,
               spanState = SpanState.COMPLETE,
               operation.returnType,
               DatabaseResponse(-1) { error.message },
               "Mongo error",
               TraceEventDirection.INBOUND)
            mapError(
               error,
               service,
               operation,
               parameters,
               aggregateJson,
               mongoConnectionConfig.connectionString.hosts.joinToString(),
               stopwatch.elapsed(),
               recordCount = -1
            )

         }

      val operationResult = buildOperationResult(
         service,
         operation,
         parameters.map { it.second },
         aggregateJson,
         mongoConnectionConfig.connectionString.hosts.joinToString(),
         elapsed = Duration.ZERO, // Happens reactive, so duration makes no sense here
         recordCount = -1
      )

      eventDispatcher.reportRemoteOperationInvoked(operationResult, queryId)
      val resultInstanceType = operation.returnType.collectionType ?: operation.returnType
      return convertToTypedInstances(resultFlux, resultInstanceType, schema, operationResult.asOperationReferenceDataSource(), traceSpan)
   }

   private fun extractAggregationParameters(
      parameters: List<Pair<Parameter, TypedInstance>>,
      operation: RemoteOperation
   ): Map<String, Any?> {
      val aggregateParams = parameters.mapIndexedNotNull { index, (param, instance) ->
         if (param.name == null) {
               logger.warn { "Parameter $index on operation ${operation.name} is not named. This cannot be used for supplying values into a Mongo aggregation template." }
               return@mapIndexedNotNull null
         }
         val name = param.name!!
         val value = instance.toRawObject()
         name to value
      }
      return aggregateParams.toMap()
   }
}
