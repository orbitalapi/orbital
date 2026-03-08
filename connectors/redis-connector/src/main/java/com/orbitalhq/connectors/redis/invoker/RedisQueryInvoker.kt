package com.orbitalhq.connectors.redis.invoker

import arrow.core.Either
import com.orbitalhq.connectors.config.redis.RedisConfiguration
import com.orbitalhq.connectors.getTaxiQlQuery
import com.orbitalhq.connectors.redis.*
import com.orbitalhq.connectors.redis.serialization.RedisJsonValueReader
import com.orbitalhq.models.DataSource
import com.orbitalhq.models.OperationResult
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.models.json.right
import com.orbitalhq.query.CacheExchange.CacheOperationVerb
import com.orbitalhq.query.QueryContextEventDispatcher
import com.orbitalhq.query.RemoteCall
import com.orbitalhq.query.ResponseMessageType
import com.orbitalhq.query.StreamErrorMessage
import com.orbitalhq.query.tracing.OperationTraceSpan
import com.orbitalhq.query.tracing.SpanState
import com.orbitalhq.query.tracing.TraceEventDirection
import com.orbitalhq.query.tracing.TracingEventKind
import com.orbitalhq.schemas.*
import io.lettuce.core.ScanArgs
import io.lettuce.core.api.StatefulRedisConnection
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flow
import lang.taxi.query.TaxiQLQueryString
import lang.taxi.query.TaxiQlQuery
import lang.taxi.types.ObjectType
import mu.KotlinLogging
import java.time.Duration
import java.time.Instant

private data class RedisCallParams(
   val keyPattern: String,
   val taxiQlQueryString: TaxiQLQueryString?,
   val parsedQuery: TaxiQlQuery?,
   val keyField: Field,
   val idLookupValue: Any?,
   val unwrappedReturnType: Type
)

class RedisQueryInvoker {
   companion object {
      private val logger = KotlinLogging.logger {}
   }

   fun plan(
      connection: StatefulRedisConnection<String, String>,
      redisConfiguration: RedisConfiguration,
      service: Service,
      operation: RemoteOperation,
      parameters: List<Pair<Parameter, TypedInstance>>,
      schema: Schema
   ): RemoteCall {
      val executionConfig = getExecutionConfig(operation, parameters, schema)
      return when {
         executionConfig.idLookupValue != null -> buildRemoteCall(
            service,
            redisConfiguration.addresses.joinToString(),
            operation,
            executionConfig.keyPattern,
            redisConfiguration.connectionName,
            executionConfig.idLookupValue.toString(),
            Duration.ZERO,
            -1,
            CacheOperationVerb.GET,
            true
         )

         queryIsFindAll(executionConfig.parsedQuery) || executionConfig.parsedQuery == null -> buildRemoteCall(
            service,
            redisConfiguration.addresses.joinToString(),
            operation,
            executionConfig.keyPattern,
            redisConfiguration.connectionName,
            "find *",
            Duration.ZERO,
            -1,
            CacheOperationVerb.READ_MANY,
            false
         )

         else -> buildRemoteCall(
            service,
            redisConfiguration.addresses.joinToString(),
            operation,
            executionConfig.keyPattern,
            redisConfiguration.connectionName,
            "criteria",
            Duration.ZERO,
            -1,
            CacheOperationVerb.READ_MANY,
            false
         )
      }
   }

   private fun getExecutionConfig(
      operation: RemoteOperation,
      parameters: List<Pair<Parameter, TypedInstance>>,
      schema: Schema
   ): RedisCallParams {
      val unwrappedReturnType = operation.returnType.collectionType ?: operation.returnType
      val keyPattern = getRedisKeyPattern(unwrappedReturnType)
      val (taxiQlQueryString, parsedQuery) = if (parameters.isNotEmpty()) {
         val (taxiQlQueryString) = parameters.getTaxiQlQuery()
         val (parsedQuery) = schema.parseQuery(taxiQlQueryString)
         taxiQlQueryString to parsedQuery
      } else null to null
      val (_, keyField) = findKeyField(unwrappedReturnType)
      val idLookupValue = parsedQuery?.let { getIdLookupValue(parsedQuery, keyField) }

      return RedisCallParams(
         keyPattern,
         taxiQlQueryString,
         parsedQuery,
         keyField,
         idLookupValue,
         unwrappedReturnType
      )
   }

   fun invoke(
      connection: StatefulRedisConnection<String, String>,
      redisConnectionConfig: RedisConfiguration,
      service: Service,
      operation: RemoteOperation,
      parameters: List<Pair<Parameter, TypedInstance>>,
      eventDispatcher: QueryContextEventDispatcher,
      queryId: String,
      queryOptions: QueryOptions,
      schema: Schema,
   ): Flow<Either<StreamErrorMessage, TypedInstance>> {
      val startTime = Instant.now()
      val traceContext = eventDispatcher.createOperationTraceSpan(service, operation, "")
      val executionConfig = getExecutionConfig(operation, parameters, schema)

      return when {
         executionConfig.idLookupValue != null -> findById(
            executionConfig.taxiQlQueryString!!,
            executionConfig.keyPattern,
            executionConfig.idLookupValue,
            connection,
            service,
            operation,
            parameters,
            redisConnectionConfig,
            startTime,
            eventDispatcher,
            queryId,
            executionConfig.unwrappedReturnType,
            schema,
            traceContext
         )

         queryIsFindAll(executionConfig.parsedQuery) || executionConfig.parsedQuery == null -> findAll(
            executionConfig.taxiQlQueryString,
            executionConfig.keyPattern,
            service,
            operation,
            parameters,
            redisConnectionConfig,
            startTime,
            connection,
            eventDispatcher,
            queryId,
            executionConfig.unwrappedReturnType,
            schema,
            traceContext
         )

         else -> findByCriteria(
            executionConfig.taxiQlQueryString!!,
            executionConfig.keyPattern,
            service,
            operation,
            parameters,
            redisConnectionConfig,
            startTime,
            connection,
            eventDispatcher,
            queryId,
            executionConfig.unwrappedReturnType,
            schema,
            traceContext
         )
      }
   }

   private fun findById(
      taxiQlQueryString: TaxiQLQueryString,
      keyPattern: String,
      idLookupValue: Any,
      connection: StatefulRedisConnection<String, String>,
      service: Service,
      operation: RemoteOperation,
      parameters: List<Pair<Parameter, TypedInstance>>,
      redisConnectionConfig: RedisConfiguration,
      startTime: Instant,
      eventDispatcher: QueryContextEventDispatcher,
      queryId: String,
      unwrappedReturnType: Type,
      schema: Schema,
      traceContext: OperationTraceSpan
   ): Flow<Either<StreamErrorMessage, TypedInstance>> {
      val redisKey = buildRedisKey(keyPattern, idLookupValue)
      logger.debug { "Query of $taxiQlQueryString converted to Redis GET for key: $redisKey" }

      return flow {
         val jsonValue = connection.sync().get(redisKey)
         val recordCount = if (jsonValue == null) 0 else 1

         traceContext.addEventNow(
            TracingEventKind.CACHE_REQUEST,
            TraceEventDirection.OUT,
            redisConnectionConfig.addresses.joinToString(),
            mapOf(
               "operation" to "GET",
               "key" to redisKey,
               "records" to recordCount
            )
         )

         if (jsonValue != null) {
            val dataSource = DataSource.cache(
               redisConnectionConfig.connectionName,
               redisConnectionConfig.driverName,
               "GET"
            )
            val typedInstanceResult = RedisJsonValueReader.toTypedInstance(
               jsonValue,
               unwrappedReturnType as ObjectType,
               schema,
               dataSource
            )

            when (typedInstanceResult) {
               is Either.Right -> {
                  emit(typedInstanceResult.value.right())
                  emitOperationResult(
                     eventDispatcher,
                     queryId,
                     service,
                     operation,
                     parameters,
                     recordCount,
                     startTime,
                     traceContext
                  )
               }
               is Either.Left -> emit(typedInstanceResult.value)
            }
         } else {
            logger.debug { "Key $redisKey not found in Redis" }
            emitOperationResult(
               eventDispatcher,
               queryId,
               service,
               operation,
               parameters,
               0,
               startTime,
               traceContext
            )
         }
      }
   }

   private fun findAll(
      taxiQlQueryString: TaxiQLQueryString?,
      keyPattern: String,
      service: Service,
      operation: RemoteOperation,
      parameters: List<Pair<Parameter, TypedInstance>>,
      redisConnectionConfig: RedisConfiguration,
      startTime: Instant,
      connection: StatefulRedisConnection<String, String>,
      eventDispatcher: QueryContextEventDispatcher,
      queryId: String,
      unwrappedReturnType: Type,
      schema: Schema,
      traceContext: OperationTraceSpan
   ): Flow<Either<StreamErrorMessage, TypedInstance>> {
      val scanPattern = getScanPattern(keyPattern)
      logger.debug { "Query of $taxiQlQueryString converted to Redis SCAN with pattern: $scanPattern" }

      return flow {
         val keys = scanKeys(connection, scanPattern)
         val recordCount = keys.size

         traceContext.addEventNow(
            TracingEventKind.CACHE_REQUEST,
            TraceEventDirection.OUT,
            redisConnectionConfig.addresses.joinToString(),
            mapOf(
               "operation" to "SCAN",
               "pattern" to scanPattern,
               "records" to recordCount
            )
         )

         val dataSource = DataSource.cache(
            redisConnectionConfig.connectionName,
            redisConnectionConfig.driverName,
            "SCAN"
         )

         // Fetch all values
         keys.asFlow()
            .flatMapConcat { key ->
               flow {
                  val jsonValue = connection.sync().get(key)
                  if (jsonValue != null) {
                     val typedInstanceResult = RedisJsonValueReader.toTypedInstance(
                        jsonValue,
                        unwrappedReturnType as ObjectType,
                        schema,
                        dataSource
                     )
                     when (typedInstanceResult) {
                        is Either.Right -> emit(typedInstanceResult.value.right())
                        is Either.Left -> emit(typedInstanceResult.value)
                     }
                  }
               }
            }
            .collect { emit(it) }

         emitOperationResult(
            eventDispatcher,
            queryId,
            service,
            operation,
            parameters,
            recordCount,
            startTime,
            traceContext
         )
      }
   }

   private fun findByCriteria(
      taxiQlQueryString: TaxiQLQueryString,
      keyPattern: String,
      service: Service,
      operation: RemoteOperation,
      parameters: List<Pair<Parameter, TypedInstance>>,
      redisConnectionConfig: RedisConfiguration,
      startTime: Instant,
      connection: StatefulRedisConnection<String, String>,
      eventDispatcher: QueryContextEventDispatcher,
      queryId: String,
      unwrappedReturnType: Type,
      schema: Schema,
      traceContext: OperationTraceSpan
   ): Flow<Either<StreamErrorMessage, TypedInstance>> {
      // For criteria-based queries, we use SCAN to get all keys and let the
      // existing expression handlers filter the results
      val scanPattern = getScanPattern(keyPattern)
      logger.debug { "Query of $taxiQlQueryString converted to Redis SCAN with pattern: $scanPattern (client-side filtering)" }

      return findAll(
         taxiQlQueryString,
         keyPattern,
         service,
         operation,
         parameters,
         redisConnectionConfig,
         startTime,
         connection,
         eventDispatcher,
         queryId,
         unwrappedReturnType,
         schema,
         traceContext
      )
   }

   private fun scanKeys(connection: StatefulRedisConnection<String, String>, pattern: String): List<String> {
      val keys = mutableListOf<String>()
      val scanArgs = ScanArgs.Builder.matches(pattern).limit(100)
      var cursor = connection.sync().scan(scanArgs)

      keys.addAll(cursor.keys)
      while (!cursor.isFinished) {
         cursor = connection.sync().scan(cursor, scanArgs)
         keys.addAll(cursor.keys)
      }

      return keys
   }

   private suspend fun emitOperationResult(
      eventDispatcher: QueryContextEventDispatcher,
      queryId: String,
      service: Service,
      operation: RemoteOperation,
      parameters: List<Pair<Parameter, TypedInstance>>,
      recordCount: Int,
      startTime: Instant,
      traceContext: OperationTraceSpan
   ) {
      val duration = Duration.between(startTime, Instant.now())
      traceContext.complete(SpanState.SUCCESS)
      eventDispatcher.publishOperation(
         ResponseMessageType.OperationResponse(
            OperationResult(
               service,
               operation,
               queryId,
               parameters,
               recordCount,
               duration
            )
         )
      )
   }

   private fun buildRemoteCall(
      service: Service,
      address: String,
      operation: RemoteOperation,
      keyPattern: String,
      connectionName: String,
      lookupDescription: String,
      duration: Duration,
      recordCount: Int,
      cacheOperation: CacheOperationVerb,
      isCacheable: Boolean
   ): RemoteCall {
      return RemoteCall.cache(
         serviceName = service.name,
         operationName = operation.name,
         connectionName = connectionName,
         resourceName = keyPattern,
         remoteAddress = address,
         remoteCallStatementDescription = lookupDescription,
         operationReturnType = operation.returnType.qualifiedName,
         duration = duration,
         recordCount = recordCount,
         cacheOperation = cacheOperation,
         isCacheable = isCacheable
      )
   }
}

private fun queryIsFindAll(query: TaxiQlQuery?): Boolean {
   return query?.let { it.projectedType is ObjectType && it.criteria.isEmpty() } ?: false
}

private fun getIdLookupValue(query: TaxiQlQuery, keyField: Field): Any? {
   val constraint = query.criteria.find { constraint ->
      constraint.targetPath == keyField.qualifiedName.longDisplayName
   }
   return constraint?.value
}
