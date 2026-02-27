package com.orbitalhq.connectors.nosql.mongodb

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.left
import com.fasterxml.jackson.databind.ObjectMapper
import com.google.common.base.Throwables
import com.mongodb.ClientSessionOptions
import com.mongodb.reactivestreams.client.ClientSession
import com.orbitalhq.connectors.config.mongodb.MongoConnectionConfiguration
import com.orbitalhq.connectors.metrics.captureMetrics
import com.orbitalhq.connectors.nosql.mongodb.BuilderUtils.extractTemplateParameters
import com.orbitalhq.metrics.MetricTags
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.query.QueryContextEventDispatcher
import com.orbitalhq.query.StreamErrorMessage
import com.orbitalhq.query.tracing.DatabaseRequest
import com.orbitalhq.query.tracing.SpanState
import com.orbitalhq.query.tracing.TraceEventDirection
import com.orbitalhq.query.tracing.TracingEventKind
import com.orbitalhq.schema.api.SchemaProvider
import com.orbitalhq.schemas.Parameter
import com.orbitalhq.schemas.RemoteOperation
import com.orbitalhq.schemas.Service
import io.micrometer.core.instrument.MeterRegistry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactor.asFlux
import mu.KotlinLogging
import org.springframework.data.mongodb.core.ReactiveMongoOperations
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.Duration
import java.util.UUID

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
   fun invokeForSinglePipeline(
      service: Service,
      operation: RemoteOperation,
      parameters: List<Pair<Parameter, TypedInstance>>,
      eventDispatcher: QueryContextEventDispatcher,
      queryId: String,
   ): Flow<Either<StreamErrorMessage, TypedInstance>> {

      val aggregateAnnotation =
         MongoConnector.Annotations.CollectionAggregation.from(operation.firstMetadata(MongoConnector.Annotations.CollectionAggregationName.parameterizedName))
      val pipeline = aggregateAnnotation.pipeline
      val (mongoConnectionConfig, reactiveMongoTemplate) = getConnectionConfigAndTemplate(service)
      val factory = reactiveMongoTemplate.mongoDatabaseFactory
      val flow = factory.getSession(ClientSessionOptions.builder().build())
         .flatMapMany { session ->
            // Single pipeline, no need for transaction, so just execute against the session
            // without starting a transaction
            val mongoOperations = reactiveMongoTemplate.withSession(session)
            executePipeline(
               service,
               operation,
               parameters,
               eventDispatcher,
               queryId,
               pipeline,
               mongoConnectionConfig,
               mongoOperations
            )
         }.asFlow()
      return flow
   }

   private fun commitTransaction(
      session: ClientSession,
      operation: RemoteOperation,
      queryId: String,
      transactional: Boolean
   ): Mono<Either<StreamErrorMessage, TypedInstance>> {
      if (!transactional) {
         logger.info { "Operation ${operation.qualifiedName} has completed - but is not transactional, so no commit required" }
         return Mono.empty()
      }

      // Lots of reactive hoop-jumping here.
      return Mono.defer {
         Mono.from(session.commitTransaction())
            .then(Mono.fromRunnable<Unit> {
               logger.info { "Committed Mongo transaction for operation=${operation.qualifiedName}, queryId=$queryId" }
            })
         // return Mono.Empty() to satisfy the  Mono<Either<StreamErrorMessage, TypedInstance>> contract,
         // which is needed to keep the contract in the calling method
      }.then(Mono.empty())
   }
   private fun abortTransaction(
      session: ClientSession,
      operation: RemoteOperation,
      queryId: String,
      error: Throwable,
      transactional: Boolean
   ): Mono<Either<StreamErrorMessage, TypedInstance>> {
      val rootCause = Throwables.getRootCause(error)
      if (!transactional) {
         logger.warn(rootCause) { "Operation ${operation.qualifiedName} failed with error: ${rootCause.message}, but is not transactional, so cannot roll back, is not transactional, so no commit required" }
         return Mono.just(
            StreamErrorMessage.fromThrowable(error, operation.returnType.paramaterizedName).left()
         )
      }

      return Mono.from(session.abortTransaction())
         .then(
            Mono.fromRunnable {
               logger.warn(rootCause) {
                  "Aborted Mongo transaction for operation=${operation.qualifiedName}, queryId=$queryId - ${rootCause.message}"
               }
               StreamErrorMessage.fromThrowable(error, operation.returnType.paramaterizedName).left()
            }
         )
   }


   /**
    * Invokes multiple aggregation pipelines in a transaction.
    * Will commit if the entire transaction succeeds, or rollback
    * (and emit an error on the error stream) if one of the pipelines fail
    */
   fun invokeForMultiplePipelinesTransactionally(
      service: Service,
      operation: RemoteOperation,
      parameters: List<Pair<Parameter, TypedInstance>>,
      eventDispatcher: QueryContextEventDispatcher,
      queryId: String,
   ): Flow<Either<StreamErrorMessage, TypedInstance>> {

      val multiAggregation =
         MongoConnector.Annotations.MultiAggregation.from(
            operation.firstMetadata(MongoConnector.Annotations.MultiAggregationName.parameterizedName)
         )

      // Short-circuit: no pipelines, nothing to do
      if (multiAggregation.pipelines.isEmpty()) {
         logger.debug { "No pipelines defined for AggregateTransaction on ${operation.qualifiedName}, returning empty flow" }
         return emptyFlow()
      }

      val (mongoConnectionConfig, reactiveMongoTemplate) = getConnectionConfigAndTemplate(service)
      val factory = reactiveMongoTemplate.mongoDatabaseFactory

      val flow = factory.getSession(ClientSessionOptions.builder().build())
         .flatMapMany { session ->
            val mongoOperations = reactiveMongoTemplate.withSession(session)

            if (multiAggregation.transactional) {
               logger.info { "Starting Mongo transaction for ${multiAggregation.pipelines.size} pipelines (operation=${operation.qualifiedName}, queryId=$queryId)" }
               session.startTransaction()
            } else {
               logger.info { "Starting Mongo pipeline collection for operation ${operation.qualifiedName}, queryId=$queryId) without transaction, as it's configued to be non-transactional" }
            }


            // Execute all but last pipeline for side-effects only
            val pipelineResults = multiAggregation.pipelines
               .dropLast(1)
               .fold(Mono.empty<Void>()) { chain, pipeline ->
                  chain.then(
                     executePipeline(
                        service,
                        operation,
                        parameters,
                        eventDispatcher,
                        queryId,
                        pipeline,
                        mongoConnectionConfig,
                        mongoOperations
                     ).then()
                  )
               }

            // Run the final pipeline and return its results
            val lastPipeline = multiAggregation.pipelines.last()
            val lastResults = executePipeline(
               service,
               operation,
               parameters,
               eventDispatcher,
               queryId,
               lastPipeline,
               mongoConnectionConfig,
               mongoOperations
            )

            pipelineResults.thenMany(lastResults)
               .concatWith(commitTransaction(session, operation, queryId, multiAggregation.transactional))
               .onErrorResume { error -> abortTransaction(session, operation, queryId, error, multiAggregation.transactional) }
         }
         .asFlow()

      return flow
   }


   /**
    * Runs the actual pipeline. This pipeline is executed within the bounds of the session
    * defined by @param mongoOperations - which allows for executing within a transaction, if desired.
    *
    * Returns a Flux<> not a Flow<>, as when we're operating inside multiple transaction phases,
    * you only want to convert the final Flux<>, and you're generally inside a Mono - so it's
    * cleaner to stay in Reactor-land until the last moment
    */
   private fun executePipeline(
      service: Service,
      operation: RemoteOperation,
      parameters: List<Pair<Parameter, TypedInstance>>,
      eventDispatcher: QueryContextEventDispatcher,
      queryId: String,
      pipeline: MongoConnector.Annotations.AggregationPipeline,
      connectionConfig: MongoConnectionConfiguration,
      mongoOperations: ReactiveMongoOperations,
   ): Flux<Either<StreamErrorMessage, TypedInstance>> {
      val schema = schemaProvider.schema
      val tags = listOf(
         MetricTags.ConnectionName.of(connectionConfig.connectionName),
         MetricTags.Operation.of(operation.name)
      )
      val collectionName = pipeline.collection
      val aggregateParams = extractTemplateParameters(parameters, operation)

      // Build the main aggregation
      val aggregation = aggregateBuilder.buildAggregation(pipeline.stages, aggregateParams)
         .getOrElse { exception ->
            val errorMessage = StreamErrorMessage.fromException(
               exception,
               MongoConnector.Annotations.CollectionAggregationName.parameterizedName
            )
            return Flux.just(errorMessage.left())
         }

      val remoteCallId = UUID.randomUUID().toString()
      val traceSpan = eventDispatcher.createOperationTraceSpan(service, operation, collectionName, remoteCallId = remoteCallId)
      val aggregateJson = aggregation.toString()

      logger.info { "Executing Mongo Aggregate: $aggregateJson" }

      val resultFlux = mongoOperations.aggregate(aggregation, collectionName, Map::class.java)
         .captureMetrics(tags, meterRegistry)
         .doOnSubscribe {
            traceSpan.emitEvent(
               TracingEventKind.OK,
               SpanState.ACTIVE,
               null,
               DatabaseRequest(connectionConfig.connectionName, "Aggregate", collectionName) { aggregateJson },
               "Aggregate",
               TraceEventDirection.OUTBOUND
            )
         }
         // Handle runtime errors
//         .onErrorResume { error ->
            // If we have onErrorStages, run them
//            if (errorPipeline != null) {
//               logger.warn(error) {
//                  "Main aggregation failed for collection=$collectionName, operation=${operation.qualifiedName}. " +
//                     "Executing onErrorStages..."
//               }
//               logger.info { "Executing onErrorStages Mongo Aggregate: $errorPipelineJson" }
//
//               mongoOperations.aggregate(errorPipeline, collectionName, Map::class.java)
//                  .captureMetrics(tags, meterRegistry)
//                  .onErrorResume { innerErr ->
//                     logger.error(innerErr) {
//                        "onErrorStages also failed for collection=$collectionName, operation=${operation.qualifiedName}"
//                     }
//                     val msg = StreamErrorMessage.fromThrowable(innerErr, operation.returnType.paramaterizedName)
//                     Flux.just(msg.left())
//                  }
//            } else {
//               // No onError handler — propagate error
//               val msg = StreamErrorMessage.fromThrowable(error, operation.returnType.paramaterizedName)
//               Flux.just(msg.left())
//            }
//            TODO("Implement error rollback")
//         }

      val operationResult = buildOperationResult(
         service,
         operation,
         parameters.map { it.second },
         aggregateJson,
         connectionConfig.connectionString.hosts.joinToString(),
         elapsed = Duration.ZERO, // Happens reactive, so duration makes no sense here
         recordCount = -1,
         parameterPairs = parameters
      )

      eventDispatcher.reportRemoteOperationInvoked(operationResult, queryId)
      val resultInstanceType = operation.returnType.collectionType ?: operation.returnType
      return convertToTypedInstances(
         resultFlux,
         resultInstanceType,
         schema,
         operationResult.asOperationReferenceDataSource(),
         traceSpan
      ).asFlux()
   }



}
