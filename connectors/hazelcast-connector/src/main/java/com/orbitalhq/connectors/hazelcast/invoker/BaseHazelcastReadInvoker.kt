package com.orbitalhq.connectors.hazelcast.invoker

import com.hazelcast.core.HazelcastInstance
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
import com.orbitalhq.query.QueryContextEventDispatcher
import com.orbitalhq.query.RemoteCall
import com.orbitalhq.query.ResponseMessageType
import com.orbitalhq.query.SqlExchange
import com.orbitalhq.schemas.Field
import com.orbitalhq.schemas.OperationKind
import com.orbitalhq.schemas.Parameter
import com.orbitalhq.schemas.QueryOptions
import com.orbitalhq.schemas.RemoteOperation
import com.orbitalhq.schemas.Schema
import com.orbitalhq.schemas.Service
import com.orbitalhq.schemas.Type
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.map
import lang.taxi.expressions.LiteralExpression
import lang.taxi.expressions.OperatorExpression
import lang.taxi.expressions.TypeExpression
import lang.taxi.query.TaxiQLQueryString
import lang.taxi.query.TaxiQlQuery
import lang.taxi.services.operations.constraints.ExpressionConstraint
import lang.taxi.types.Arrays
import lang.taxi.types.FormulaOperator
import lang.taxi.types.ObjectType
import mu.KotlinLogging
import java.time.Duration
import java.time.Instant

abstract class BaseHazelcastReadInvoker {
   companion object {
      private val logger = KotlinLogging.logger {}
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
   ): Flow<TypedInstance> {
      val startTime = Instant.now()
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
      return when {
         idLookupValue != null -> findById(
            taxiQlQueryString!!,
            mapName,
            idLookupValue,
            map,
            service,
            operation,
            parameters,
            hazelcastConnectionConfig,
            startTime,
            eventDispatcher,
            queryId,
            unwrappedReturnType,
            schema,
         )

         queryIsFindAll(parsedQuery) || parsedQuery == null && operation.operationKind == OperationKind.Stream -> findAll(
            taxiQlQueryString,
            mapName,
            service,
            operation,
            parameters,
            hazelcastConnectionConfig,
            startTime,
            map,
            eventDispatcher,
            queryId,
            unwrappedReturnType,
            schema,
         )

         filterCriteria != null -> findByCriteria(
            taxiQlQueryString!!,
            parsedQuery,
            filterCriteria,
            mapName,
            service,
            operation,
            parameters,
            hazelcastConnectionConfig,
            startTime,
            map,
            eventDispatcher,
            queryId,
            unwrappedReturnType,
            schema,
         )

         else -> error("Unsupported read scenario found in query: $taxiQlQueryString")
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
   ): Flow<TypedInstance> {
      val predicate = ExpressionToPredicateConverter.convert(taxiQlQuery.discoveryType!!, filterCriteria)
      val (rawResults, resultSize) = buildFlowOfCriteriaSearch(
         taxiQlQueryString,
         operation,
         map,
         predicate,
      )
      val operationResult = buildOperationResult(
         service,
         operation,
         parameters.map { it.second },
         hazelcastConnectionConfig,
         predicate.toString(),
         elapsed = Duration.between(startTime, Instant.now()),
         resultSize,
         "SELECT"
      )
      eventDispatcher.reportRemoteOperationInvoked(operationResult, queryId)
      return flowOfResult(
         rawResults,
         unwrappedReturnType.taxiType as ObjectType,
         schema,
         operationResult.asOperationReferenceDataSource()
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
   ): Flow<TypedInstance> {
      logger.debug { "Query of $taxiQlQueryString converted to Id lookup against map $mapName with key $idLookupValue" }
      val (rawResultFlow, recordCount) = buildFlowById(taxiQlQueryString, idLookupValue, map, operation)

      val result = buildOperationResult(
         service,
         operation,
         parameters.map { it.second },
         hazelcastConnectionConfig,
         idLookupValue.toString(),
         elapsed = Duration.between(startTime, Instant.now()),
         recordCount,
         "LOOKUP"
      )
      if (recordCount == 0) {
         logger.debug { "Lookup using key $idLookupValue against map $mapName returned null" }
      }
      eventDispatcher.reportRemoteOperationInvoked(result, queryId)
      return flowOfResult(
         rawResultFlow,
         unwrappedReturnType.taxiType as ObjectType,
         schema,
         result.asOperationReferenceDataSource()
      )
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
   ): Flow<TypedInstance> {
      val (rawResultsFlow, resultSize) = buildFlowOfFullMap(map, operation, taxiQlQueryString, )
      val result = buildOperationResult(
         service,
         operation,
         parameters.map { it.second },
         hazelcastConnectionConfig,
         "find *",
         elapsed = Duration.between(startTime, Instant.now()),
         resultSize,
         "FIND_ALL"
      )
      eventDispatcher.reportRemoteOperationInvoked(result, queryId)
      return flowOfResult(
         rawResultsFlow,
         unwrappedReturnType.taxiType as ObjectType,
         schema,
         result.asOperationReferenceDataSource()
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
   ): Flow<TypedInstance> {
      fun readValue(value:Any?):List<TypedInstance> {
         return when (value) {
            null -> listOf( TypedNull.create(schema.type(memberType), dataSource))
            is DeserializedGenericRecord -> listOf(GenericRecordReader.toTypedInstance(value, memberType, schema, dataSource))
            is QueryResultCollection<*> -> {
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

   fun buildOperationResult(
      service: Service,
      operation: RemoteOperation,
      parameters: List<TypedInstance>,
      connectionConfig: HazelcastConfiguration,
      sql: String,
      elapsed: Duration,
      recordCount: Int,
      verb: String = "SELECT"
   ): OperationResult {
      val remoteCall =
         buildRemoteCall(service, connectionConfig.addresses.joinToString(), operation, sql, elapsed, recordCount, verb)
      return OperationResult.fromTypedInstances(
         parameters,
         remoteCall
      )
   }

   private fun buildRemoteCall(
      service: Service,
      jdbcUrl: String,
      operation: RemoteOperation,
      sql: String,
      elapsed: Duration,
      recordCount: Int,
      verb: String
   ) = RemoteCall(
      service = service.name,
      address = jdbcUrl,
      operation = operation.name,
      responseTypeName = operation.returnType.name,
      requestBody = sql,
      durationMs = elapsed.toMillis(),
      timestamp = Instant.now(),
      // If we implement streaming database queries, this will change
      responseMessageType = ResponseMessageType.FULL,
      // Feels like capturing the results are a bad idea.  Can revisit if there's a use-case
      response = null,
      exchange = SqlExchange(
         sql = sql,
         recordCount = recordCount,
         verb = verb
      ),

      )

}
