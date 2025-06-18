package com.orbitalhq.connectors.nosql.mongodb

import arrow.core.Either
import com.fasterxml.jackson.databind.ObjectMapper
import com.orbitalhq.metrics.MetricTags
import com.orbitalhq.models.DataSourceUpdater
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.query.QueryContextEventDispatcher
import com.orbitalhq.query.StreamErrorMessage
import com.orbitalhq.query.tracing.SpanState
import com.orbitalhq.query.tracing.DatabaseRequest
import com.orbitalhq.query.tracing.DatabaseResponse
import com.orbitalhq.query.tracing.TracingEventKind
import com.orbitalhq.schema.api.SchemaProvider
import com.orbitalhq.schemas.Parameter
import com.orbitalhq.schemas.QueryOptions
import com.orbitalhq.schemas.RemoteOperation
import com.orbitalhq.schemas.Service
import com.orbitalhq.schemas.fqn
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.reactive.asFlow
import mu.KotlinLogging

private val logger = KotlinLogging.logger { }

class MongoMutatingQueryInvoker(
   connectionFactory: MongoConnectionFactory,
   schemaProvider: SchemaProvider,
   private val meterRegistry: MeterRegistry,
   private val objectMapper: ObjectMapper
) : MongoBaseInvoker(connectionFactory, schemaProvider, objectMapper) {

   suspend fun invoke(
      service: Service,
      operation: RemoteOperation,
      parameters: List<Pair<Parameter, TypedInstance>>,
      eventDispatcher: QueryContextEventDispatcher,
      queryId: String,
      queryOptions: QueryOptions
   ): Flow<Either<StreamErrorMessage, TypedInstance>> {
      val schema = schemaProvider.schema


      require(operation.parameters.size == 1) { "Operations annotated with ${MongoConnector.Annotations.UpsertOperationAnnotationName} should accept exactly one type" }
      val inputType = operation.parameters.single().type.let { type -> type.collectionType ?: type }

      require(inputType.hasMetadata(MongoConnector.Annotations.Collection.NAME.fqn()))
      { "The input type into an ${MongoConnector.Annotations.UpsertOperationAnnotationName} operation should be a type with a ${MongoConnector.Annotations.Collection.NAME.fqn()} annotation" }

      val (param, input) = parameters.singleOrNull()
         ?: error("Expected a single parameter, but received ${parameters.size}")
      val collectionName = inputType.taxiType.collectionNameOrTypeName()
      val traceContext = eventDispatcher.createOperationTraceSpan(service, operation, collectionName)
      val (connectionConfig, reactiveMongoTemplate) = getConnectionConfigAndTemplate(service)
      val recordToWrite = parameters[0].second
      val documentMap = typedInstanceToMap(recordToWrite)
      val upsertDefinition = MongoCriteriaGenerator.upsertFor(recordToWrite, documentMap)
      val tags = listOf(
         MetricTags.ConnectionName.of(connectionConfig.connectionName),
         MetricTags.TableName.of(collectionName)
      )
      var updateCounter: Counter? = null
      var verb: String? = null
      var traceRequestMetadata: DatabaseRequest? = null
      val upsertMono = if (upsertDefinition == null) {
         verb = "save"
         traceRequestMetadata = DatabaseRequest(
            connectionConfig.connectionName,
            "save",
            collectionName,
         ) { objectMapper.writeValueAsString(documentMap) }
         val result = reactiveMongoTemplate.save(documentMap, collectionName)
            .map { 1L /* update count */ to it }
         updateCounter = meterRegistry.counter("orbital.connections.mongo.updates", tags)
         result
      } else {
         verb = "upsert"
         updateCounter = meterRegistry.counter("orbital.connections.mongo.updates", tags)
         traceRequestMetadata = DatabaseRequest(
            connectionConfig.connectionName,
            "upsert",
            collectionName,
         ) {
            val upsert =
               mapOf("query" to upsertDefinition.first.queryObject, "update" to upsertDefinition.second.updateObject)
            objectMapper.writeValueAsString(upsert)
         }
         reactiveMongoTemplate.upsert(upsertDefinition.first, upsertDefinition.second, collectionName)
            .map { upsertResult ->
               // Looks like we get a 0 for modified count if this was an insert.
               // To verify, we report 1 if the upsertedId != null && modifiedCount == 0,
               // as there must've been an insert if an id was assigned.

               val modifiedCount =
                  if (upsertResult.modifiedCount == 0L && upsertResult.upsertedId != null && upsertResult.wasAcknowledged()) {
                     1L
                  } else {
                     upsertResult.modifiedCount
                  }
               if (modifiedCount == 0L) {
                  upsertDefinition
                  logger.warn { "Upsert to Mongo collection ${connectionConfig.connectionName} / $collectionName reported 0 records updated" }
               }
               traceContext.emitEvent(
                  TracingEventKind.OK,
                  SpanState.COMPLETE,
                  null,
                  DatabaseResponse(
                     modifiedCount,
                  ) { objectMapper.writeValueAsString(upsertResult) },
                  "Upsert"
               )
               modifiedCount to documentMap
            }
      }

      return upsertMono
         .elapsed()
         .doOnSubscribe {
            traceContext.emitEvent(
               kind = TracingEventKind.OK,
               spanState = SpanState.ACTIVE,
               payloadType = null,
               exchangeMetadata = traceRequestMetadata,
               verb = "Upsert"
            )
         }
         .map { durationAndData ->
            val duration = durationAndData.t1
            val (updateCount, data) = durationAndData.t2
            logger.info { "Mongo $verb call against mongo collectin ${connectionConfig.connectionName} / $collectionName completed  in ${duration}ms affecting $updateCount records" }
            updateCounter?.increment(updateCount.toDouble())
            val resultEvent = traceContext.emitEvent(
               TracingEventKind.OK,
               SpanState.COMPLETE,
               operation.returnType,
               DatabaseResponse(updateCount) { objectMapper.writeValueAsString(data) },
               "Upsert complete"
            )

            val operationResult = buildOperationResult(
               service,
               operation,
               listOf(input),
               "upsert",
               connectionConfig.connectionString.hosts.joinToString(),
               java.time.Duration.ofMillis(duration),
               recordCount = 1
            )
            eventDispatcher.reportRemoteOperationInvoked(operationResult, queryId)
            val resultTypedInstance =
               mapToTypedInstance(
                  data,
                  inputType,
                  schema,
                  operationResult.asOperationReferenceDataSource(traceEventId = resultEvent.idSet),
               )
            resultTypedInstance.map { typedInstance ->
               DataSourceUpdater.update(typedInstance, operationResult.asOperationReferenceDataSource())
            }

         }
         .doOnError { error ->
            traceContext.emitEvent(
               TracingEventKind.ERROR,
               SpanState.COMPLETE,
               null,
               DatabaseResponse(-1) { error.message },
               "Read error"
            )
         }
         .asFlow()

   }
}

/**
 * The difference between insert and save operations is that a save operation performs an insert if the object is not already present.
 * When inserting or saving, if the Id property is not set, the assumption is that its value will be auto-generated by the database.
 * Consequently, for auto-generation of an ObjectId to succeed,
 * the type of the Id property or field in your class must be a String, an ObjectId, or a BigInteger.
 */
