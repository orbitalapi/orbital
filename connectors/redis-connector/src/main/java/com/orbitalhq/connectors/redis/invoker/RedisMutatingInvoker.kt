package com.orbitalhq.connectors.redis.invoker

import arrow.core.Either
import com.orbitalhq.connectors.config.redis.RedisConfiguration
import com.orbitalhq.connectors.redis.*
import com.orbitalhq.connectors.redis.serialization.RedisJsonValueWriter
import com.orbitalhq.models.DataSource
import com.orbitalhq.models.DataSourceUpdater
import com.orbitalhq.models.OperationResult
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.models.TypedNull
import com.orbitalhq.models.TypedObject
import com.orbitalhq.models.json.Jackson
import com.orbitalhq.models.json.right
import com.orbitalhq.query.CacheExchange
import com.orbitalhq.query.QueryContextEventDispatcher
import com.orbitalhq.query.RemoteCall
import com.orbitalhq.query.ResponseMessageType
import com.orbitalhq.query.StreamErrorMessage
import com.orbitalhq.query.tracing.CacheRequest
import com.orbitalhq.query.tracing.CacheResponse
import com.orbitalhq.query.tracing.OperationTraceSpan
import com.orbitalhq.query.tracing.SpanState
import com.orbitalhq.query.tracing.TraceEventDirection
import com.orbitalhq.query.tracing.TracingEventKind
import com.orbitalhq.schemas.Parameter
import com.orbitalhq.schemas.QueryOptions
import com.orbitalhq.schemas.RemoteOperation
import com.orbitalhq.schemas.Schema
import com.orbitalhq.schemas.Service
import io.lettuce.core.SetArgs
import io.lettuce.core.api.StatefulRedisConnection
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import lang.taxi.types.PrimitiveType
import mu.KotlinLogging
import java.time.Duration
import java.time.Instant

class RedisMutatingInvoker {

   companion object {
      private val logger = KotlinLogging.logger {}
   }

   fun plan(
      connection: StatefulRedisConnection<String, String>,
      config: RedisConfiguration,
      service: Service,
      operation: RemoteOperation,
      parameters: List<Pair<Parameter, TypedInstance>>,
      schema: Schema
   ): RemoteCall {
      val (_, valueToSave) = parameters[0]
      val keyPattern = getRedisKeyPattern(valueToSave.type)

      return buildRemoteCall(
         keyPattern,
         service,
         config.addresses.joinToString(),
         operation,
         "Available at execution time",
         Duration.ZERO,
         -1,
         getVerb(operation),
         config.connectionName
      )
   }

   private fun doUpsert(
      connection: StatefulRedisConnection<String, String>,
      parameters: List<Pair<Parameter, TypedInstance>>,
      schema: Schema,
      operation: RemoteOperation,
      reportResult: (String, String, Int, CacheExchange.CacheOperationVerb) -> DataSource,
      traceContext: OperationTraceSpan,
      connectionName: String
   ): Flow<Either<StreamErrorMessage, TypedInstance>> {
      val (_, valueToSave) = parameters[0]
      val keyPattern = getRedisKeyPattern(valueToSave.type)
      require(valueToSave is TypedObject) { "Only TypedObjects are supported - Need to add support for ${valueToSave::class.simpleName}" }

      val (key, jsonValue) = RedisJsonValueWriter.getJsonStringAndKey(valueToSave, schema)
      val redisKey = buildRedisKey(keyPattern, key)

      logger.debug { "Setting value in Redis with key $redisKey" }

      traceContext.emitEvent(
         kind = TracingEventKind.OK,
         spanState = SpanState.ACTIVE,
         payloadType = parameters[0].first.type,
         exchangeMetadata = CacheRequest(keyPattern, CacheExchange.CacheOperationVerb.UPDATE, connectionName) {
            "Upsert key $redisKey to ${Jackson.defaultObjectMapper.writeValueAsString(valueToSave.toRawObject())}"
         },
         verb = "Upsert",
         direction = TraceEventDirection.OUTBOUND
      )

      // Get TTL from annotation or operation
      val ttlSeconds = getTTLSeconds(operation, valueToSave.type)

      if (ttlSeconds != null) {
         connection.sync().setex(redisKey, ttlSeconds.toLong(), jsonValue)
         logger.debug { "Set key $redisKey with TTL of $ttlSeconds seconds" }
      } else {
         connection.sync().set(redisKey, jsonValue)
         logger.debug { "Set key $redisKey without TTL" }
      }

      val resultEvent = traceContext.emitEvent(
         TracingEventKind.OK,
         SpanState.COMPLETE,
         valueToSave.type,
         CacheResponse(1) { "Upserted 1 record to Redis with key $redisKey" },
         "Upsert response",
         direction = TraceEventDirection.INBOUND
      )

      val dataSource = reportResult("SET $redisKey", keyPattern, 1, CacheExchange.CacheOperationVerb.UPDATE)
      val updatedValue = DataSourceUpdater.update(valueToSave, dataSource)
      return flowOf(updatedValue.right())
   }

   private fun getTTLSeconds(operation: RemoteOperation, type: com.orbitalhq.schemas.Type): Int? {
      // Check operation annotation first
      if (operation.hasMetadata(RedisTaxi.Annotations.RedisUpsertOperation.parameterizedName)) {
         val upsertAnnotation = operation.firstMetadata(RedisTaxi.Annotations.RedisUpsertOperation.parameterizedName)
         val ttl = upsertAnnotation.params["ttlSeconds"] as? Int
         if (ttl != null) return ttl
      }

      // Fall back to type annotation
      return getTTLSeconds(type)
   }

   private fun doDelete(
      connection: StatefulRedisConnection<String, String>,
      parameters: List<Pair<Parameter, TypedInstance>>,
      schema: Schema,
      operation: RemoteOperation,
      reportAndGenerateDataSource: (String, String, Int, CacheExchange.CacheOperationVerb) -> DataSource,
      traceContext: OperationTraceSpan,
      connectionName: String
   ): Flow<Either<StreamErrorMessage, TypedInstance>> {
      val deleteAnnotation = operation.firstMetadata(RedisTaxi.Annotations.RedisDeleteOperation.parameterizedName)
      val keyPattern = deleteAnnotation.params["keyPattern"] as String?
         ?: error("Operation ${operation.qualifiedName.parameterizedName} does not declare a keyPattern")

      traceContext.emitEvent(
         kind = TracingEventKind.OK,
         spanState = SpanState.ACTIVE,
         payloadType = parameters.firstOrNull()?.first?.type,
         exchangeMetadata = CacheRequest(keyPattern, CacheExchange.CacheOperationVerb.DELETE, connectionName),
         verb = "Delete",
         direction = TraceEventDirection.INBOUND
      )

      val deleteKey = parameters.singleOrNull()?.second
      return if (deleteKey == null) {
         deleteByPattern(keyPattern, connection, reportAndGenerateDataSource, schema, traceContext)
      } else {
         deleteByKey(deleteKey, keyPattern, connection, reportAndGenerateDataSource, schema, operation, traceContext)
      }
   }

   private fun deleteByKey(
      deleteKey: TypedInstance,
      keyPattern: String,
      connection: StatefulRedisConnection<String, String>,
      reportAndGenerateDataSource: (String, String, Int, CacheExchange.CacheOperationVerb) -> DataSource,
      schema: Schema,
      operation: RemoteOperation,
      traceContext: OperationTraceSpan
   ): Flow<Either<StreamErrorMessage, TypedInstance>> {

      val keyValue = deleteKey.toRawObject() ?: error("Cannot delete from Redis as provided key was null")
      val redisKey = buildRedisKey(keyPattern, keyValue)
      val deletedCount = connection.sync().del(redisKey)
      val recordCount = deletedCount.toInt()

      val resultEvent = traceContext.emitEvent(
         TracingEventKind.OK,
         SpanState.COMPLETE,
         operation.returnType,
         CacheResponse(recordCount) { "Deleted $recordCount record from Redis with key $redisKey" },
         "Delete response",
         direction = TraceEventDirection.INBOUND
      )

      val dataSource = reportAndGenerateDataSource(
         "DEL $redisKey",
         keyPattern,
         recordCount,
         CacheExchange.CacheOperationVerb.DELETE,
      )

      // For delete operations, we return TypedNull since the value no longer exists
      val result = TypedNull.create(operation.returnType, source = dataSource)
      return flowOf(result.right())
   }

   private fun deleteByPattern(
      keyPattern: String,
      connection: StatefulRedisConnection<String, String>,
      reportAndGenerateDataSource: (String, String, Int, CacheExchange.CacheOperationVerb) -> DataSource,
      schema: Schema,
      traceContext: OperationTraceSpan
   ): Flow<Either<StreamErrorMessage, TypedInstance>> {
      logger.info { "Performing deleteByPattern with pattern $keyPattern" }

      // Scan for all keys matching the pattern
      val scanPattern = if (keyPattern.contains("{")) {
         getScanPattern(keyPattern)
      } else {
         keyPattern
      }

      val keys = scanKeys(connection, scanPattern)
      val deletedCount = if (keys.isNotEmpty()) {
         connection.sync().del(*keys.toTypedArray())
      } else {
         0L
      }

      val recordCount = deletedCount.toInt()

      val resultEvent = traceContext.emitEvent(
         TracingEventKind.OK,
         SpanState.COMPLETE,
         schema.type(PrimitiveType.VOID),
         CacheResponse(recordCount) { "Deleted $recordCount records from Redis matching pattern $scanPattern" },
         "Delete all response",
         direction = TraceEventDirection.INBOUND
      )

      val dataSource = reportAndGenerateDataSource("DEL $scanPattern", keyPattern, recordCount, CacheExchange.CacheOperationVerb.DELETE)
      return flowOf(TypedNull.create(schema.type(PrimitiveType.VOID), source = dataSource).right())
   }

   private fun scanKeys(connection: StatefulRedisConnection<String, String>, pattern: String): List<String> {
      val keys = mutableListOf<String>()
      val scanArgs = io.lettuce.core.ScanArgs.Builder.matches(pattern).limit(100)
      var cursor = connection.sync().scan(scanArgs)

      keys.addAll(cursor.keys)
      while (!cursor.isFinished) {
         cursor = connection.sync().scan(cursor, scanArgs)
         keys.addAll(cursor.keys)
      }

      return keys
   }

   fun invoke(
      connection: StatefulRedisConnection<String, String>,
      service: Service,
      operation: RemoteOperation,
      parameters: List<Pair<Parameter, TypedInstance>>,
      redisConnectionConfig: RedisConfiguration,
      eventDispatcher: QueryContextEventDispatcher,
      queryId: String,
      queryOptions: QueryOptions,
      schema: Schema
   ): Flow<Either<StreamErrorMessage, TypedInstance>> {
      val startTime = Instant.now()
      val traceContext = eventDispatcher.createOperationTraceSpan(service, operation, "")

      fun reportResult(
         command: String,
         keyPattern: String,
         resultSize: Int,
         verb: CacheExchange.CacheOperationVerb
      ): DataSource {
         val result = buildOperationResult(
            service,
            operation,
            keyPattern,
            parameters.map { it.second },
            redisConnectionConfig,
            command,
            Duration.between(startTime, Instant.now()),
            resultSize,
            verb
         )
         eventDispatcher.reportRemoteOperationInvoked(result, queryId)
         return result.asOperationReferenceDataSource()
      }

      return when (getVerb(operation)) {
         CacheExchange.CacheOperationVerb.UPDATE -> doUpsert(
            connection,
            parameters,
            schema,
            operation,
            ::reportResult,
            traceContext,
            redisConnectionConfig.connectionName
         )

         CacheExchange.CacheOperationVerb.DELETE -> doDelete(
            connection,
            parameters,
            schema,
            operation,
            ::reportResult,
            traceContext,
            redisConnectionConfig.connectionName
         )

         else -> error("Unexpected type of mutation for Redis: ${operation.qualifiedName.parameterizedName}")
      }
   }

   private fun getVerb(operation: RemoteOperation): CacheExchange.CacheOperationVerb {
      return when {
         operation.hasMetadata(RedisTaxi.Annotations.RedisUpsertOperation.parameterizedName) -> CacheExchange.CacheOperationVerb.UPDATE
         operation.hasMetadata(RedisTaxi.Annotations.RedisDeleteOperation.parameterizedName) -> CacheExchange.CacheOperationVerb.DELETE
         else -> error("Unexpected type of mutation for Redis: ${operation.qualifiedName.parameterizedName}")
      }
   }

   private fun buildOperationResult(
      service: Service,
      operation: RemoteOperation,
      keyPattern: String,
      parameters: List<TypedInstance>,
      connectionConfig: RedisConfiguration,
      command: String,
      elapsed: Duration,
      recordCount: Int,
      verb: CacheExchange.CacheOperationVerb
   ): OperationResult {
      val remoteCall = buildRemoteCall(
         keyPattern,
         service,
         connectionConfig.addresses.joinToString(),
         operation,
         command,
         elapsed,
         recordCount,
         verb,
         connectionConfig.connectionName
      )
      return OperationResult.fromTypedInstances(
         parameters,
         remoteCall
      )
   }

   private fun buildRemoteCall(
      keyPattern: String,
      service: Service,
      redisAddresses: String,
      operation: RemoteOperation,
      command: String,
      elapsed: Duration,
      recordCount: Int,
      verb: CacheExchange.CacheOperationVerb,
      connectionName: String
   ) = RemoteCall(
      service = service.name,
      address = redisAddresses,
      operation = operation.name,
      responseTypeName = operation.returnType.name,
      requestBody = command,
      durationMs = elapsed.toMillis(),
      timestamp = Instant.now(),
      responseMessageType = ResponseMessageType.FULL,
      response = null,
      exchange = CacheExchange(
         connectionName = connectionName,
         cacheName = keyPattern,
         cacheKeyOrStatement = command,
         cacheType = CacheExchange.CacheType.Redis,
         recordCount = recordCount,
         verb = verb
      ),
   )
}
