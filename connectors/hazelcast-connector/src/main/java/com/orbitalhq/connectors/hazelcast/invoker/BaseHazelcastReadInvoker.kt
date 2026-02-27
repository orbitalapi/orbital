package com.orbitalhq.connectors.hazelcast.invoker

import arrow.core.Either
import com.hazelcast.core.HazelcastInstance
import com.hazelcast.core.HazelcastJsonValue
import com.hazelcast.internal.serialization.impl.compact.DeserializedGenericRecord
import com.hazelcast.map.IMap
import com.hazelcast.map.impl.query.QueryResultCollection
import com.hazelcast.query.Predicate
import com.orbitalhq.connectors.config.hazelcast.HazelcastConfiguration
import com.orbitalhq.connectors.getTaxiQlQuery
import com.orbitalhq.models.DataSource
import com.orbitalhq.models.OperationResult
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.models.TypedNull
import com.orbitalhq.models.json.right
import com.orbitalhq.query.CacheExchange
import com.orbitalhq.query.CacheExchange.CacheOperationVerb
import com.orbitalhq.query.QueryContextEventDispatcher
import com.orbitalhq.query.RemoteCall
import com.orbitalhq.query.RemoteCallExchangeMetadata
import com.orbitalhq.query.ResponseMessageType
import com.orbitalhq.query.StreamErrorMessage
import com.orbitalhq.query.tracing.CacheRequest
import com.orbitalhq.query.tracing.CacheResponse
import com.orbitalhq.query.tracing.OperationTraceSpan
import com.orbitalhq.query.tracing.SpanState
import com.orbitalhq.query.tracing.TraceEventDirection
import com.orbitalhq.query.tracing.TracingEventKind
import com.orbitalhq.schemas.Field
import com.orbitalhq.schemas.OperationInvocationException
import com.orbitalhq.schemas.OperationKind
import com.orbitalhq.schemas.Parameter
import com.orbitalhq.schemas.QueryOptions
import com.orbitalhq.schemas.RemoteOperation
import com.orbitalhq.schemas.Schema
import com.orbitalhq.schemas.Service
import com.orbitalhq.schemas.Type
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flatMapConcat
import lang.taxi.expressions.LiteralExpression
import lang.taxi.expressions.OperatorExpression
import lang.taxi.expressions.TypeExpression
import java.util.UUID
import lang.taxi.query.TaxiQLQueryString
import lang.taxi.query.TaxiQlQuery
import lang.taxi.services.operations.constraints.ExpressionConstraint
import lang.taxi.types.Arrays
import lang.taxi.types.FormulaOperator
import lang.taxi.types.ObjectType
import mu.KotlinLogging
import java.time.Duration
import java.time.Instant

private data class HazelcastCallParams(
   val mapName: String,
   val map: IMap<Any, Any>,
   val taxiQlQueryString: TaxiQLQueryString?,
   val parsedQuery: TaxiQlQuery?,
   val keyField: Field,
   val idLookupValue: Any?,
   val filterCriteria: OperatorExpression?,
   val unwrappedReturnType: Type
)

abstract class BaseHazelcastReadInvoker {
   companion object {
      private val logger = KotlinLogging.logger {}
   }


   fun plan(
      hazelcastInstance: HazelcastInstance,
      hazelcastConfiguration: HazelcastConfiguration,
      service: Service,
      operation: RemoteOperation,
      parameters: List<Pair<Parameter, TypedInstance>>,
      schema: Schema
   ):RemoteCall {
      val executionConfig = getExecutionConfig(operation, hazelcastInstance, parameters, schema)
      return when {
         executionConfig.idLookupValue != null -> buildRemoteCall(
            service,
            hazelcastConfiguration.addresses.joinToString(),
            operation,
            executionConfig.mapName,
            hazelcastConfiguration.connectionName,
            executionConfig.idLookupValue.toString(),
            Duration.ZERO,
            -1,
            CacheOperationVerb.GET,
            true
         )

         queryIsFindAll(executionConfig.parsedQuery) || executionConfig.parsedQuery == null && operation.operationKind == OperationKind.Stream -> buildRemoteCall(
            service,
            hazelcastConfiguration.addresses.joinToString(),
            operation,
            executionConfig.mapName,
            hazelcastConfiguration.connectionName,
            "find *",
            Duration.ZERO,
            -1,
            CacheOperationVerb.GET_ALL,
            true
         )

         executionConfig.filterCriteria != null -> {
            val predicate = ExpressionToPredicateConverter.convert(executionConfig.parsedQuery!!.discoveryType!!, executionConfig.filterCriteria)
            buildRemoteCall(
               service,
               hazelcastConfiguration.addresses.joinToString(),
               operation,
               executionConfig.mapName,
               hazelcastConfiguration.connectionName,
               predicate.toString(),
               Duration.ZERO,
               -1,
               CacheOperationVerb.QUERY,
               true
            )
         }

         else -> error("Unsupported read scenario found in query: ${executionConfig.taxiQlQueryString}")
      }
   }

   private fun getExecutionConfig(
      operation: RemoteOperation,
      hazelcastInstance: HazelcastInstance,
      parameters: List<Pair<Parameter, TypedInstance>>,
      schema: Schema
   ): HazelcastCallParams {
      val unwrappedReturnType = operation.returnType.collectionType ?: operation.returnType
      val mapName = getMapName(unwrappedReturnType)
      val map = hazelcastInstance.getMap<Any, Any>(mapName)
      val (taxiQlQueryString, parsedQuery) = if (parameters.isNotEmpty()) {
         val (taxiQlQueryString) = parameters.getTaxiQlQuery()
         val (parsedQuery) = schema.parseQuery(taxiQlQueryString)
         taxiQlQueryString to parsedQuery
      } else null to null
      val (_, keyField) = findKeyField(unwrappedReturnType)
      val idLookupValue = parsedQuery?.let { getIdLookupValue(parsedQuery, keyField) }
      val filterCriteria = parsedQuery?.let { getFilterCriteriaOrNull(parsedQuery) }

      return HazelcastCallParams(
         mapName,
         map,
         taxiQlQueryString,
         parsedQuery,
         keyField,
         idLookupValue,
         filterCriteria,
         unwrappedReturnType
      )
   }

   fun invoke(
      hazelcastInstance: HazelcastInstance,
      hazelcastConnectionConfig: HazelcastConfiguration,
      service: Service,
      operation: RemoteOperation,
      parameters: List<Pair<Parameter, TypedInstance>>,
      eventDispatcher: QueryContextEventDispatcher,
      queryId: String,
      queryOptions: QueryOptions,
      schema: Schema,
   ): Flow<Either<StreamErrorMessage, TypedInstance>> {
      val startTime = Instant.now()
      val remoteCallId = UUID.randomUUID().toString()
      val traceContext = eventDispatcher.createOperationTraceSpan(service, operation, "", remoteCallId = remoteCallId)
      val executionConfig = getExecutionConfig(operation, hazelcastInstance, parameters, schema)
      return when {
         executionConfig.idLookupValue != null -> findById(
            executionConfig.taxiQlQueryString!!,
            executionConfig.mapName,
            executionConfig.idLookupValue,
            executionConfig.map,
            service,
            operation,
            parameters,
            hazelcastConnectionConfig,
            startTime,
            eventDispatcher,
            queryId,
            executionConfig.unwrappedReturnType,
            schema,
            traceContext
         )

         queryIsFindAll(executionConfig.parsedQuery) || executionConfig.parsedQuery == null && operation.operationKind == OperationKind.Stream -> findAll(
            executionConfig.taxiQlQueryString,
            executionConfig.mapName,
            service,
            operation,
            parameters,
            hazelcastConnectionConfig,
            startTime,
            executionConfig.map,
            eventDispatcher,
            queryId,
            executionConfig.unwrappedReturnType,
            schema,
            traceContext
         )

         executionConfig.filterCriteria != null -> findByCriteria(
            executionConfig.taxiQlQueryString!!,
            executionConfig.parsedQuery!!,
            executionConfig.filterCriteria,
            executionConfig.mapName,
            service,
            operation,
            parameters,
            hazelcastConnectionConfig,
            startTime,
            executionConfig.map,
            eventDispatcher,
            queryId,
            executionConfig.unwrappedReturnType,
            schema,
            traceContext
         )

         else -> error("Unsupported read scenario found in query: ${executionConfig.taxiQlQueryString}")
      }
   }

   private fun findByCriteria(
      taxiQlQueryString: TaxiQLQueryString,
      taxiQlQuery: TaxiQlQuery,
      filterCriteria: OperatorExpression,
      mapName: String,
      service: Service,
      operation: RemoteOperation,
      parameters: List<Pair<Parameter, TypedInstance>>,
      hazelcastConnectionConfig: HazelcastConfiguration,
      startTime: Instant,
      map: IMap<Any, Any>,
      eventDispatcher: QueryContextEventDispatcher,
      queryId: String,
      unwrappedReturnType: Type,
      schema: Schema,
      traceContext: OperationTraceSpan
   ): Flow<Either<StreamErrorMessage, TypedInstance>> {
      val predicate = ExpressionToPredicateConverter.convert(taxiQlQuery.discoveryType!!, filterCriteria)

      traceContext.emitEvent(
         kind = TracingEventKind.OK,
         spanState = SpanState.ACTIVE,
         payloadType = operation.returnType,
         exchangeMetadata = CacheRequest(mapName, CacheOperationVerb.QUERY, hazelcastConnectionConfig.connectionName) { filterCriteria.asTaxi()},
         verb = "Query",
         direction = TraceEventDirection.OUTBOUND
      )

      val (rawResults, resultSize) = buildFlowOfCriteriaSearch(
         taxiQlQueryString,
         operation,
         map,
         predicate,
      )
      val (taxiQuery, constructedQueryDataSource) = parameters.getTaxiQlQuery()

      val resultEvent = traceContext.emitEvent(
         TracingEventKind.OK,
         SpanState.COMPLETE,
         operation.returnType,
         CacheResponse(resultSize) { "Retrieved $resultSize records from map $mapName with predicate $predicate" },
         "Query response",
         direction = TraceEventDirection.INBOUND
      )

      val isSuccessful = resultSize > 0
      val operationResult = buildOperationResult(
         service,
         operation,
         constructedQueryDataSource.inputs,
         hazelcastConnectionConfig,
         mapName,
         predicate.toString(),
         elapsed = Duration.between(startTime, Instant.now()),
         resultSize,
         CacheOperationVerb.QUERY,
         isSuccessful,
         parameterPairs = parameters
      )
      eventDispatcher.reportRemoteOperationInvoked(operationResult, queryId)
      return flowOfResult(
         rawResults,
         unwrappedReturnType.taxiType as ObjectType,
         schema,
         operationResult.asOperationReferenceDataSource(resultEvent.idSet)
      )
   }

   abstract fun buildFlowOfCriteriaSearch(
      taxiQlQueryString: TaxiQLQueryString,
      operation: RemoteOperation,
      map: IMap<Any, Any>,
      predicate: Predicate<Any, Any>,
   ): Pair<Flow<Any?>, Int>

   abstract fun buildFlowById(
      taxiQlQueryString: TaxiQLQueryString,
      idLookupValue: Any,
      map: IMap<Any, Any>,
      operation: RemoteOperation,
   ): Pair<Flow<Any?>, Int>

   private fun findById(
      taxiQlQueryString: TaxiQLQueryString,
      mapName: String,
      idLookupValue: Any,
      map: IMap<Any, Any>,
      service: Service,
      operation: RemoteOperation,
      parameters: List<Pair<Parameter, TypedInstance>>,
      hazelcastConnectionConfig: HazelcastConfiguration,
      startTime: Instant?,
      eventDispatcher: QueryContextEventDispatcher,
      queryId: String,
      unwrappedReturnType: Type,
      schema: Schema,
      traceContext: OperationTraceSpan
   ): Flow<Either<StreamErrorMessage, TypedInstance>> {
      logger.debug { "Query of $taxiQlQueryString converted to Id lookup against map $mapName with key $idLookupValue" }

      traceContext.emitEvent(
         kind = TracingEventKind.OK,
         spanState = SpanState.ACTIVE,
         payloadType = operation.returnType,
         exchangeMetadata = CacheRequest(mapName, CacheOperationVerb.GET, hazelcastConnectionConfig.connectionName) { "Key = $idLookupValue" },
         verb = "Get",
         direction = TraceEventDirection.OUTBOUND
      )

      val (rawResultFlow, recordCount) = buildFlowById(taxiQlQueryString, idLookupValue, map, operation)

      val (taxiQuery, constructedQueryDataSource) = parameters.getTaxiQlQuery()

      val resultEvent = traceContext.emitEvent(
         TracingEventKind.OK,
         SpanState.COMPLETE,
         operation.returnType,
         CacheResponse(recordCount) { "Retrieved $recordCount record from map $mapName with key $idLookupValue" },
         "Get response",
         direction = TraceEventDirection.INBOUND
      )

      val isSuccess = recordCount > 0
      val result = buildOperationResult(
         service,
         operation,
         constructedQueryDataSource.inputs,
         hazelcastConnectionConfig,
         mapName,
         idLookupValue.toString(),
         elapsed = Duration.between(startTime, Instant.now()),
         recordCount,
         CacheOperationVerb.GET,
         isSuccess,
         parameterPairs = parameters
      )
      eventDispatcher.reportRemoteOperationInvoked(result, queryId)
      val errorMessage = "Lookup using key $idLookupValue against map $mapName returned null"
      if (false) {
         logger.debug { errorMessage }
         throw OperationInvocationException(
            errorMessage,
            404,
            result.remoteCall,
            parameters
         )
      } else {
         return flowOfResult(
            rawResultFlow,
            unwrappedReturnType.taxiType as ObjectType,
            schema,
            result.asOperationReferenceDataSource(resultEvent.idSet)
         )
      }
   }

   private fun findAll(
      taxiQlQueryString: TaxiQLQueryString?,
      mapName: String,
      service: Service,
      operation: RemoteOperation,
      parameters: List<Pair<Parameter, TypedInstance>>,
      hazelcastConnectionConfig: HazelcastConfiguration,
      startTime: Instant?,
      map: IMap<Any, Any>,
      eventDispatcher: QueryContextEventDispatcher,
      queryId: String,
      unwrappedReturnType: Type,
      schema: Schema,
      traceContext: OperationTraceSpan
   ): Flow<Either<StreamErrorMessage, TypedInstance>> {
      traceContext.emitEvent(
         kind = TracingEventKind.OK,
         spanState = SpanState.ACTIVE,
         payloadType = operation.returnType,
         exchangeMetadata = CacheRequest(mapName, CacheOperationVerb.GET_ALL, hazelcastConnectionConfig.connectionName) { "Find all"},
         verb = "GetAll",
         direction = TraceEventDirection.OUTBOUND
      )

      val (rawResultsFlow, resultSize) = buildFlowOfFullMap(map, operation, taxiQlQueryString)

      // Note - generally for a findAll(), there are no parameters, so an empty list
      // is the appropriate value.
      // If, for some reason, there are params, it should be a TaxiQL query, so we
      // need to extract the inputs from that.
      val operationParams = if (parameters.isEmpty()) {
         emptyList()
      } else {
         val (taxiQuery, constructedQueryDataSource) = parameters.getTaxiQlQuery()
         constructedQueryDataSource.inputs
      }

      val resultEvent = traceContext.emitEvent(
         TracingEventKind.OK,
         SpanState.COMPLETE,
         operation.returnType,
         CacheResponse(resultSize) { "Retrieved all $resultSize records from map $mapName" },
         "GetAll response",
         direction = TraceEventDirection.INBOUND
      )

      val result = buildOperationResult(
         service,
         operation,
         operationParams,
         hazelcastConnectionConfig,
         mapName,
         "find *",
         elapsed = Duration.between(startTime, Instant.now()),
         resultSize,
         CacheOperationVerb.GET_ALL,
         true,
         parameterPairs = parameters
      )
      eventDispatcher.reportRemoteOperationInvoked(result, queryId)
      return flowOfResult(
         rawResultsFlow,
         unwrappedReturnType.taxiType as ObjectType,
         schema,
         result.asOperationReferenceDataSource(resultEvent.idSet)
      )
   }

   abstract fun buildFlowOfFullMap(
      map: IMap<Any, Any>,
      operation: RemoteOperation,
      taxiQlQueryString: TaxiQLQueryString?,
   ): Pair<Flow<Any?>, Int>

   private fun flowOfResult(
      values: Flow<Any?>,
      memberType: ObjectType,
      schema: Schema,
      dataSource: DataSource
   ): Flow<Either<StreamErrorMessage, TypedInstance>> {
      fun readValue(value: Any?): List<Either<StreamErrorMessage, TypedInstance>> {
         return when (value) {
            null -> listOf(TypedNull.create(schema.type(memberType), dataSource).right())
            is DeserializedGenericRecord -> listOf(
               GenericRecordReader.toTypedInstance(
                  value,
                  memberType,
                  schema,
                  dataSource
               )
            )

            is HazelcastJsonValue -> listOf(
               HazelcastJsonValueReader.toTypedInstance(
                  value,
                  memberType,
                  schema,
                  dataSource
               )
            )

            is QueryResultCollection<*> -> {
               value.flatMap { readValue(it) }
            }

            is List<*> -> {
               value.flatMap { readValue(it) }
            }

            else -> error("No way to deserialize read value from Hazelcast map with type ${value::class.simpleName}")
         }
      }

      val typedInstances = values.flatMapConcat { value ->
         readValue(value).asFlow()
      }
      return typedInstances
   }


   private fun queryIsFindAll(parsedQuery: TaxiQlQuery?): Boolean {
      val type = parsedQuery?.discoveryType?.type ?: return false
      return Arrays.isArray(type) && parsedQuery.discoveryType!!.constraints.isEmpty()
   }

   /**
    * If the query is a single equality constraint against the map key,
    * returns the value being looked for.
    * eg:
    * find { Film(FilmId == 123) }
    *
    * Otherwise, returns null
    */
   private fun getIdLookupValue(parsedQuery: TaxiQlQuery, keyField: Field): Any? {

      // Check to see if there's a single type, with a single constraint,
      // and that constraint is IdField == ALiteralValue.
      // eg: find { Film(FilmId == 123) }
      val typeConstraintExpression = getFilterCriteriaOrNull(parsedQuery) ?: return null
      if (typeConstraintExpression.operator !== FormulaOperator.Equal) return null

      // At this point, we know it's a single operator expression.
      // Verify its FilmId == 123 or 123 == FilmId
      val sides = setOf(typeConstraintExpression.lhs, typeConstraintExpression.rhs)
      if (!sides.any { it is LiteralExpression }) return null
      if (!sides.any { it is TypeExpression && it.type.toQualifiedName().parameterizedName == keyField.type.parameterizedName }) return null
      return sides.filterIsInstance<LiteralExpression>()
         .single()
         .value

   }

   private fun getFilterCriteriaOrNull(parsedQuery: TaxiQlQuery): OperatorExpression? {
      val discoveryType = parsedQuery.discoveryType ?: return null
      val expression = discoveryType.expression
      if (expression !is TypeExpression) return null
      if (expression.constraints.size != 1) return null
      val typeConstraint = expression.constraints.single()
      if (typeConstraint !is ExpressionConstraint) return null
      val typeConstraintExpression = typeConstraint.expression
      if (typeConstraintExpression !is OperatorExpression) return null
      return typeConstraintExpression
   }

   private fun buildOperationResult(
      service: Service,
      operation: RemoteOperation,
      parameters: List<TypedInstance>,
      connectionConfig: HazelcastConfiguration,
      cacheName: String,
      sql: String,
      elapsed: Duration,
      recordCount: Int,
      verb: CacheOperationVerb,
      success: Boolean,
      parameterPairs: List<Pair<Parameter, TypedInstance>> = emptyList()
   ): OperationResult {
      val remoteCall =
         buildRemoteCall(
            service,
            connectionConfig.addresses.joinToString(),
            operation,
            cacheName,
            connectionConfig.connectionName,
            sql,
            elapsed,
            recordCount,
            verb,
            success,
            parameterPairs
         )
      return OperationResult.fromTypedInstances(
         parameters,
         remoteCall
      )
   }

   private fun buildRemoteCall(
      service: Service,
      address: String,
      operation: RemoteOperation,
      cacheName: String,
      connectionName: String,
      sql: String,
      elapsed: Duration,
      recordCount: Int,
      verb: CacheOperationVerb,
      success: Boolean,
      parameterPairs: List<Pair<Parameter, TypedInstance>> = emptyList()
   ) = RemoteCall(
      service = service.name,
      address = address,
      operation = operation.name,
      responseTypeName = operation.returnType.name,
      requestBody = sql,
      durationMs = elapsed.toMillis(),
      timestamp = Instant.now(),
      responseMessageType = ResponseMessageType.FULL,
      // Feels like capturing the results are a bad idea.  Can revisit if there's a use-case
      response = null,
      exchange = CacheExchange(
         cacheKeyOrStatement = sql,
         recordCount = recordCount,
         verb = verb,
         connectionName = connectionName,
         cacheName = cacheName,
         cacheType = CacheExchange.CacheType.Hazelcast,
         parameters = RemoteCallExchangeMetadata.convertParametersToJson(parameterPairs)
      ),
      isFailed = !success
   )

}
