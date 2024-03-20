package com.orbitalhq.connectors.nosql.mongodb

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.common.base.Stopwatch
import com.orbitalhq.connectors.getTypesToFind
import com.orbitalhq.models.OperationResult
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.query.ConstructedQueryDataSource
import com.orbitalhq.query.QueryContextEventDispatcher
import com.orbitalhq.query.RemoteCall
import com.orbitalhq.query.ResponseMessageType
import com.orbitalhq.query.SqlExchange
import com.orbitalhq.schema.api.SchemaProvider
import com.orbitalhq.schemas.Parameter
import com.orbitalhq.schemas.QueryOptions
import com.orbitalhq.schemas.RemoteOperation
import com.orbitalhq.schemas.Service
import com.orbitalhq.utils.withQueryId
import kotlinx.coroutines.flow.Flow
import mu.KotlinLogging
import org.springframework.data.mongodb.core.query.Query
import java.time.Duration
import java.time.Instant


private val logger = KotlinLogging.logger { }

class MongoReadOnlyQueryInvoker(
     connectionFactory: MongoConnectionFactory,
     schemaProvider: SchemaProvider
): MongoBaseInvoker(connectionFactory, schemaProvider) {
   suspend fun invoke(
      service: Service,
      operation: RemoteOperation,
      parameters: List<Pair<Parameter, TypedInstance>>,
      eventDispatcher: QueryContextEventDispatcher,
      queryId: String,
      queryOptions: QueryOptions
   ): Flow<TypedInstance> {
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

      logger.withQueryId(queryId).debug { "Starting Mongo Query" }
      val stopwatch = Stopwatch.createStarted()
      val resultFlux = if (criterias.isEmpty()) {
         reactiveMongoTemplate.findAll(Map::class.java, typesToCollectionNames.values.first())
      } else {
         logger.withQueryId(queryId).info { "Using the Mongo Criteria => ${criterias.first().criteriaObject.toJson()}" }
         reactiveMongoTemplate.find(
            Query().addCriteria(criterias.first()),
            Map::class.java,
            typesToCollectionNames.values.first()
         )
      }
      val elapsed = stopwatch.elapsed()
      logger.withQueryId(queryId).debug { "Mongo Query completed in $elapsed" }
      val operationResult = buildOperationResult(
         service,
         operation,
         constructedQueryDataSource.inputs,
         if (criterias.isNotEmpty()) criterias.first().toString() else SelectAllCriteria,
         mongoConnectionConfig.connectionString.hosts.joinToString(),
         elapsed,
         recordCount = -1
      )

      eventDispatcher.reportRemoteOperationInvoked(operationResult, queryId)
      return convertToTypedInstances(resultFlux, query, schema, operationResult.asOperationReferenceDataSource())
   }
}
