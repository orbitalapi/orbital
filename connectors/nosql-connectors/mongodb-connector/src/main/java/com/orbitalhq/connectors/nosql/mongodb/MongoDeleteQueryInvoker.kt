package com.orbitalhq.connectors.nosql.mongodb

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.getOrElse
import arrow.core.left
import arrow.core.right
import arrow.core.sequence
import com.fasterxml.jackson.databind.ObjectMapper
import com.google.common.base.Stopwatch
import com.mongodb.client.result.DeleteResult
import com.orbitalhq.connectors.config.mongodb.MongoConnectionConfiguration
import com.orbitalhq.connectors.metrics.captureMetrics
import com.orbitalhq.metrics.MetricTags
import com.orbitalhq.models.DataSourceUpdater
import com.orbitalhq.models.TypedCollection
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.models.TypedObject
import com.orbitalhq.query.QueryContextEventDispatcher
import com.orbitalhq.query.StreamErrorMessage
import com.orbitalhq.query.tracing.DatabaseRequest
import com.orbitalhq.query.tracing.DatabaseResponse
import com.orbitalhq.query.tracing.OperationTraceSpan
import com.orbitalhq.query.tracing.SpanState
import com.orbitalhq.query.tracing.TraceEventDirection
import com.orbitalhq.query.tracing.TracingEventKind
import com.orbitalhq.schema.api.SchemaProvider
import com.orbitalhq.schemas.Parameter
import com.orbitalhq.schemas.QueryOptions
import com.orbitalhq.schemas.RemoteOperation
import com.orbitalhq.schemas.Schema
import com.orbitalhq.schemas.Service
import com.orbitalhq.schemas.fqn
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tag
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.reactive.asFlow
import mu.KotlinLogging
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import java.time.Duration
import java.util.UUID

/**
 * Handles the @DeleteOperation annotations,
 * which provide type-safe Orbital deletes
 */
class MongoDeleteQueryInvoker(
   connectionFactory: MongoConnectionFactory,
   schemaProvider: SchemaProvider,
   private val meterRegistry: MeterRegistry,
   private val objectMapper: ObjectMapper
) : MongoBaseDeleteInvoker(connectionFactory, schemaProvider, meterRegistry, objectMapper) {
   companion object {
      private val logger = KotlinLogging.logger {}
   }

   suspend fun invoke(
      service: Service,
      operation: RemoteOperation,
      parameters: List<Pair<Parameter, TypedInstance>>,
      eventDispatcher: QueryContextEventDispatcher,
      queryId: String,
      queryOptions: QueryOptions
   ): Flow<Either<StreamErrorMessage, TypedInstance>> {
      val schema = schemaProvider.schema

      require(operation.parameters.size == 1) {
         "Operations annotated with ${MongoConnector.Annotations.DeleteOperationName} should accept exactly one parameter"
      }

      val inputParameter = operation.parameters.single()
      val inputType = inputParameter.type.collectionType ?: inputParameter.type

      require(inputType.hasMetadata(MongoConnector.Annotations.Collection.NAME.fqn())) {
         "The input type into a ${MongoConnector.Annotations.DeleteOperationName} operation should be a type with a ${MongoConnector.Annotations.Collection.NAME.fqn()} annotation"
      }

      val (param, input) = parameters.singleOrNull()
         ?: error("Expected a single parameter, but received ${parameters.size}")

      val collectionName = inputType.taxiType.collectionNameOrTypeName()
      val remoteCallId = UUID.randomUUID().toString()
      val traceContext = eventDispatcher.createOperationTraceSpan(service, operation, collectionName, remoteCallId = remoteCallId)
      val (connectionConfig, reactiveMongoTemplate) = getConnectionConfigAndTemplate(service)

      val tags = tags(connectionConfig, collectionName, operation)
      val deleteCounter: Counter = meterRegistry.counter("orbital.connections.mongo.deletes", tags)

      val stopwatch = Stopwatch.createStarted()


      val recordsToDeleteQuery = getIdsToDelete(input)
         .flatMap { idCriteria ->
            idCriteria.reduceWithOrOperator()?.right()
               ?: StreamErrorMessage.illegalArgument(
                  "No criteria were generated for deleting against type ${inputType.name.parameterizedName}",
                  inputType.name.parameterizedName
               )
                  .left()
         }
         .map { idCriteria -> Query(idCriteria) }
         .getOrElse { return flowOf(it.left()) }


      val queryJson = recordsToDeleteQuery.queryObject.toJson()

      logger.info { "Executing deleteMany with criteria: $queryJson" }

      val traceRequestMetadata = DatabaseRequest(
         connectionConfig.connectionName,
         "Delete",
         collectionName,
      ) { queryJson }

      val flow = reactiveMongoTemplate.remove(recordsToDeleteQuery, collectionName)
         .captureMetrics(tags, meterRegistry)
         .doOnSubscribe { traceDeleteStart(traceContext, traceRequestMetadata) }
         .map { deleteResult ->
            handleDeleteResult(
               deleteResult,
               stopwatch,
               connectionConfig,
               collectionName,
               deleteCounter,
               traceContext,
               operation,
               service,
               listOf(input),
               eventDispatcher,
               queryId,
               schema
            )
         }
         .doOnError { error ->
            traceError(traceContext, error)
         }
         .asFlow()

      return flow
   }


   private fun getIdsToDelete(input: TypedInstance): Either<StreamErrorMessage, List<Criteria>> {
      return if (input is TypedCollection) {
         input.map { getIdsToDelete(it) }
            .sequence() // collects all the Eithers<>, stopping on the first Either.Left
            .map { it.flatten() }
      } else {
         if (input !is TypedObject) {
            StreamErrorMessage.illegalArgument(
               "Expected a TypedObject or a TypedCollection, but got ${input::class.simpleName}",
               MongoConnector.Annotations.DeleteOperation.NAME
            )
               .left()
         } else {
            val criteria = MongoCriteriaGenerator.getIdentifyingCriteriaForInstance(input)
            if (criteria == null) {
               StreamErrorMessage.illegalArgument(
                  "Cannot generate delete operation for type ${input.type.name.shortDisplayName} as no identifying attributes are present - add either @Id or @UniqueIndex annotations",
                  input.type.paramaterizedName
               )
                  .left()
            } else {
               listOf(criteria).right()
            }
         }


      }
   }


}

fun deleteResult(deletedCount: Long): Map<String, Any> = mapOf("deletedCount" to deletedCount.toInt())
