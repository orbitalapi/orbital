package com.orbitalhq.connectors.nosql.mongodb

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.left
import com.fasterxml.jackson.databind.ObjectMapper
import com.google.common.base.Stopwatch
import com.orbitalhq.connectors.metrics.captureMetrics
import com.orbitalhq.connectors.nosql.mongodb.BuilderUtils.extractTemplateParameters
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.query.QueryContextEventDispatcher
import com.orbitalhq.query.StreamErrorMessage
import com.orbitalhq.query.tracing.DatabaseRequest
import com.orbitalhq.schema.api.SchemaProvider
import com.orbitalhq.schemas.Parameter
import com.orbitalhq.schemas.QueryOptions
import com.orbitalhq.schemas.RemoteOperation
import com.orbitalhq.schemas.Service
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.reactive.asFlow
import mu.KotlinLogging
import java.util.UUID

/**
 * Handles the @MongoDelete, which providers
 * richer, mongo-native delete syntax (vs @DeleteOperation)
 */
class MongoDeleteByQueryInvoker(
   connectionFactory: MongoConnectionFactory,
   schemaProvider: SchemaProvider,
   private val meterRegistry: MeterRegistry,
   private val objectMapper: ObjectMapper
) : MongoBaseDeleteInvoker(connectionFactory, schemaProvider, meterRegistry, objectMapper) {
   companion object {
      private val logger = KotlinLogging.logger {}
   }

   private val filterBuilder = MongoFilterBuilder(objectMapper)

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


      val deleteAnnotation =
         MongoConnector.Annotations.DeleteByQuery.from(operation.firstMetadata(MongoConnector.Annotations.DeleteByQueryName.parameterizedName))
      val collectionName = deleteAnnotation.collection
      val tags = tags(mongoConnectionConfig, collectionName, operation)
      val deleteCounter: Counter = meterRegistry.counter("orbital.connections.mongo.deletes", tags)
      val operationParams = extractTemplateParameters(parameters, operation)
      val filter = filterBuilder.buildFilter(deleteAnnotation.filter, operationParams)
         .getOrElse { exception ->
            val errorMessage = StreamErrorMessage.fromException(
               exception,
               MongoConnector.Annotations.CollectionAggregationName.parameterizedName
            )
            return flowOf(errorMessage.left())
         }

      val remoteCallId = UUID.randomUUID().toString()
      val traceContext = eventDispatcher.createOperationTraceSpan(service, operation, collectionName, remoteCallId = remoteCallId)
      val stopwatch = Stopwatch.createStarted()
      val filterJson = filter.toString()

      logger.info { "Executing @MongoDelete with filter: $filterJson" }

      val traceRequestMetadata = DatabaseRequest(
         mongoConnectionConfig.connectionName,
         "Delete",
         collectionName,
      ) { filterJson }

      return reactiveMongoTemplate.remove(filter, collectionName)
         .captureMetrics(tags, meterRegistry)
         .doOnSubscribe { traceDeleteStart(traceContext, traceRequestMetadata) }
         .map { deleteResult ->
            handleDeleteResult(
               deleteResult,
               stopwatch,
               mongoConnectionConfig,
               collectionName,
               deleteCounter,
               traceContext,
               operation,
               service,
               emptyList(),
               eventDispatcher,
               queryId,
               schema
            )
         }
         .doOnError { error ->
            traceError(traceContext, error)
         }
         .asFlow()
   }
}
