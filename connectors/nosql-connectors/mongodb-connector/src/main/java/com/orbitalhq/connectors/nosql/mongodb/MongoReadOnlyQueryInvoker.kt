package com.orbitalhq.connectors.nosql.mongodb

import arrow.core.Either
import com.fasterxml.jackson.databind.ObjectMapper
import com.google.common.base.Stopwatch
import com.orbitalhq.connectors.getTypesToFind
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.query.ConstructedQueryDataSource
import com.orbitalhq.query.QueryContextEventDispatcher
import com.orbitalhq.query.StreamErrorMessage
import com.orbitalhq.query.tracing.DatabaseRequest
import com.orbitalhq.query.tracing.DatabaseResponse
import com.orbitalhq.query.tracing.SpanState
import com.orbitalhq.query.tracing.TracingEventKind
import com.orbitalhq.schema.api.SchemaProvider
import com.orbitalhq.schemas.Parameter
import com.orbitalhq.schemas.QueryOptions
import com.orbitalhq.schemas.RemoteOperation
import com.orbitalhq.schemas.Service
import kotlinx.coroutines.flow.Flow
import mu.KotlinLogging
import org.springframework.data.mongodb.core.query.Query


private val logger = KotlinLogging.logger { }

class MongoReadOnlyQueryInvoker(
   connectionFactory: MongoConnectionFactory,
   schemaProvider: SchemaProvider,
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
      val (mongoConnectionConfig, reactiveMongoTemplate) = getConnectionConfigAndTemplate(service)
      val schema = schemaProvider.schema
      val taxiSchema = schema.taxi
      val (taxiQuery, constructedQueryDataSource) = parameters[0].second.let { it.value as String to it.source as ConstructedQueryDataSource }
      val (query, _) = schema.parseQuery(taxiQuery)
      val typesToFind = getTypesToFind(query, taxiSchema)
      val typesToCollectionNames = MongoQueryHelpers.getCollectionNames(typesToFind)
      val criterias = MongoCriteriaGenerator(schema).crtieriaFor(query)
      if (typesToCollectionNames.size > 1) {
         error("Mongo Joins are not yet supported - can only select from a single collection")
      }
      val collectionName = typesToCollectionNames.values.first()
      val traceSpan = eventDispatcher.createOperationTraceSpan(service, operation, collectionName)
      val criteriaJson = if (criterias.isEmpty()) SelectAllCriteria else criterias.first().criteriaObject.toJson()


      logger.debug { "Starting Mongo Query" }
      val stopwatch = Stopwatch.createStarted()

      val resultFlux = if (criterias.isEmpty()) {
         reactiveMongoTemplate.findAll(Map::class.java, collectionName)
      } else {
         logger.info { "Using the Mongo Criteria => $criteriaJson" }
         reactiveMongoTemplate.find(
            Query().addCriteria(criterias.first()),
            Map::class.java,
            collectionName
         )
            .doOnSubscribe {
               traceSpan.emitEvent(
                  TracingEventKind.OK,
                  SpanState.ACTIVE,
                  null,
                  DatabaseRequest(mongoConnectionConfig.connectionName, "find", collectionName) {
                     objectMapper.writeValueAsString(criterias.map { it.criteriaObject })
                  },
                  "Select")
            }
            .onErrorMap { error ->
               traceSpan.emitEvent(
                  TracingEventKind.ERROR,
                  spanState = SpanState.COMPLETE,
                  operation.returnType,
                  DatabaseResponse(-1) { error.message },
                  "Select error")
               mapError(
                  error,
                  service,
                  operation,
                  parameters,
                  criteriaJson,
                  mongoConnectionConfig.connectionString.hosts.joinToString(),
                  stopwatch.elapsed(),
                  recordCount = -1
               )

            }
      }
      val elapsed = stopwatch.elapsed()
      logger.debug { "Mongo Query completed in $elapsed" }
      val operationResult = buildOperationResult(
         service,
         operation,
         constructedQueryDataSource.inputs,
         criteriaJson,
         mongoConnectionConfig.connectionString.hosts.joinToString(),
         elapsed,
         recordCount = -1
      )

      eventDispatcher.reportRemoteOperationInvoked(operationResult, queryId)
      return convertToTypedInstances(resultFlux, query, schema, operationResult.asOperationReferenceDataSource(), traceSpan)
   }


}
