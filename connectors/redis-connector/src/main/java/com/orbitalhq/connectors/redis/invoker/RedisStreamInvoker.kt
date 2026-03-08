package com.orbitalhq.connectors.redis.invoker

import arrow.core.Either
import com.orbitalhq.connectors.config.redis.RedisConfiguration
import com.orbitalhq.connectors.redis.*
import com.orbitalhq.connectors.redis.serialization.RedisJsonValueReader
import com.orbitalhq.models.DataSource
import com.orbitalhq.models.OperationResult
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.models.json.right
import com.orbitalhq.query.CacheExchange
import com.orbitalhq.query.QueryContextEventDispatcher
import com.orbitalhq.query.RemoteCall
import com.orbitalhq.query.ResponseMessageType
import com.orbitalhq.query.StreamErrorMessage
import com.orbitalhq.query.tracing.OperationTraceSpan
import com.orbitalhq.query.tracing.SpanState
import com.orbitalhq.query.tracing.TraceEventDirection
import com.orbitalhq.query.tracing.TracingEventKind
import com.orbitalhq.schemas.*
import io.lettuce.core.RedisPubSubAdapter
import io.lettuce.core.StreamMessage
import io.lettuce.core.XReadArgs
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import lang.taxi.query.TaxiQLQueryString
import lang.taxi.query.TaxiQlQuery
import lang.taxi.types.ObjectType
import mu.KotlinLogging
import java.time.Duration
import java.time.Instant
import java.util.concurrent.atomic.AtomicBoolean

class RedisStreamInvoker {
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
      val unwrappedReturnType = operation.returnType.collectionType ?: operation.returnType

      // Determine if this is a Redis Stream or Pub/Sub stream
      val isRedisStream = unwrappedReturnType.hasMetadata(RedisTaxi.Annotations.RedisStreamName.parameterizedName)
      val isPubSub = unwrappedReturnType.hasMetadata(RedisTaxi.Annotations.RedisPubSubChannel.parameterizedName)

      val resourceName = when {
         isRedisStream -> {
            val metadata = unwrappedReturnType.getMetadata(RedisTaxi.Annotations.RedisStreamName)
            "stream:${metadata.params["name"]}"
         }
         isPubSub -> {
            val metadata = unwrappedReturnType.getMetadata(RedisTaxi.Annotations.RedisPubSubChannel)
            "pubsub:${metadata.params["channel"]}"
         }
         else -> {
            val keyPattern = getRedisKeyPattern(unwrappedReturnType)
            "keyspace:$keyPattern"
         }
      }

      return RemoteCall.cache(
         serviceName = service.name,
         operationName = operation.name,
         connectionName = redisConfiguration.connectionName,
         resourceName = resourceName,
         remoteAddress = redisConfiguration.addresses.joinToString(),
         remoteCallStatementDescription = "stream",
         operationReturnType = operation.returnType.qualifiedName,
         duration = Duration.ZERO,
         recordCount = -1,
         cacheOperation = CacheExchange.CacheOperationVerb.READ_MANY,
         isCacheable = false
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
      val unwrappedReturnType = operation.returnType.collectionType ?: operation.returnType

      // Determine the streaming mechanism based on annotations
      return when {
         unwrappedReturnType.hasMetadata(RedisTaxi.Annotations.RedisStreamName.parameterizedName) -> {
            invokeRedisStream(
               connection,
               redisConnectionConfig,
               service,
               operation,
               parameters,
               eventDispatcher,
               queryId,
               schema,
               unwrappedReturnType,
               traceContext
            )
         }
         unwrappedReturnType.hasMetadata(RedisTaxi.Annotations.RedisPubSubChannel.parameterizedName) -> {
            invokePubSub(
               connection,
               redisConnectionConfig,
               service,
               operation,
               parameters,
               eventDispatcher,
               queryId,
               schema,
               unwrappedReturnType,
               traceContext
            )
         }
         else -> {
            // Default: use keyspace notifications
            invokeKeyspaceNotifications(
               connection,
               redisConnectionConfig,
               service,
               operation,
               parameters,
               eventDispatcher,
               queryId,
               schema,
               unwrappedReturnType,
               traceContext
            )
         }
      }
   }

   private fun invokeRedisStream(
      connection: StatefulRedisConnection<String, String>,
      redisConnectionConfig: RedisConfiguration,
      service: Service,
      operation: RemoteOperation,
      parameters: List<Pair<Parameter, TypedInstance>>,
      eventDispatcher: QueryContextEventDispatcher,
      queryId: String,
      schema: Schema,
      unwrappedReturnType: Type,
      traceContext: OperationTraceSpan
   ): Flow<Either<StreamErrorMessage, TypedInstance>> {
      val streamMetadata = unwrappedReturnType.getMetadata(RedisTaxi.Annotations.RedisStreamName)
      val streamName = streamMetadata.params["name"] as String

      logger.debug { "Starting Redis Stream consumer for stream: $streamName" }

      traceContext.addEventNow(
         TracingEventKind.CACHE_REQUEST,
         TraceEventDirection.OUT,
         redisConnectionConfig.addresses.joinToString(),
         mapOf(
            "operation" to "XREAD",
            "stream" to streamName,
            "mode" to "streaming"
         )
      )

      return flow {
         val dataSource = DataSource.cache(
            redisConnectionConfig.connectionName,
            redisConnectionConfig.driverName,
            "XREAD"
         )

         var lastId = "$" // Start from latest

         while (currentCoroutineContext().isActive) {
            try {
               val messages = connection.sync().xread(
                  XReadArgs.Builder.block(1000).count(10),
                  XReadArgs.StreamOffset.from(streamName, lastId)
               )

               for (message in messages) {
                  val body = message.body
                  // Assume the stream message has a "data" field with JSON
                  val jsonData = body["data"] ?: continue

                  val typedInstanceResult = RedisJsonValueReader.toTypedInstance(
                     jsonData,
                     unwrappedReturnType as ObjectType,
                     schema,
                     dataSource
                  )

                  when (typedInstanceResult) {
                     is Either.Right -> emit(typedInstanceResult.value.right())
                     is Either.Left -> emit(typedInstanceResult.value)
                  }

                  lastId = message.id
               }
            } catch (e: Exception) {
               if (e is CancellationException) throw e
               logger.error(e) { "Error reading from Redis Stream $streamName" }
            }
         }

         traceContext.complete(SpanState.SUCCESS)
      }
   }

   private fun invokePubSub(
      connection: StatefulRedisConnection<String, String>,
      redisConnectionConfig: RedisConfiguration,
      service: Service,
      operation: RemoteOperation,
      parameters: List<Pair<Parameter, TypedInstance>>,
      eventDispatcher: QueryContextEventDispatcher,
      queryId: String,
      schema: Schema,
      unwrappedReturnType: Type,
      traceContext: OperationTraceSpan
   ): Flow<Either<StreamErrorMessage, TypedInstance>> {
      val pubSubMetadata = unwrappedReturnType.getMetadata(RedisTaxi.Annotations.RedisPubSubChannel)
      val channel = pubSubMetadata.params["channel"] as String

      logger.debug { "Starting Redis Pub/Sub consumer for channel: $channel" }

      traceContext.addEventNow(
         TracingEventKind.CACHE_REQUEST,
         TraceEventDirection.OUT,
         redisConnectionConfig.addresses.joinToString(),
         mapOf(
            "operation" to "SUBSCRIBE",
            "channel" to channel,
            "mode" to "streaming"
         )
      )

      val flowSink = MutableSharedFlow<Either<StreamErrorMessage, TypedInstance>>()
      val pubSubConnection = connection.statefulConnection.connectPubSub()
      val isActive = AtomicBoolean(true)

      val dataSource = DataSource.cache(
         redisConnectionConfig.connectionName,
         redisConnectionConfig.driverName,
         "PUBSUB"
      )

      pubSubConnection.addListener(object : RedisPubSubAdapter<String, String>() {
         override fun message(channel: String, message: String) {
            if (!isActive.get()) return

            val typedInstanceResult = RedisJsonValueReader.toTypedInstance(
               message,
               unwrappedReturnType as ObjectType,
               schema,
               dataSource
            )

            runBlocking {
               when (typedInstanceResult) {
                  is Either.Right -> flowSink.emit(typedInstanceResult.value.right())
                  is Either.Left -> flowSink.emit(typedInstanceResult.value)
               }
            }
         }
      })

      pubSubConnection.async().subscribe(channel)

      return flowSink.asSharedFlow()
         .onCompletion {
            isActive.set(false)
            pubSubConnection.async().unsubscribe(channel)
            pubSubConnection.close()
            traceContext.complete(SpanState.SUCCESS)
            logger.debug { "Redis Pub/Sub listener for channel $channel closed" }
         }
   }

   private fun invokeKeyspaceNotifications(
      connection: StatefulRedisConnection<String, String>,
      redisConnectionConfig: RedisConfiguration,
      service: Service,
      operation: RemoteOperation,
      parameters: List<Pair<Parameter, TypedInstance>>,
      eventDispatcher: QueryContextEventDispatcher,
      queryId: String,
      schema: Schema,
      unwrappedReturnType: Type,
      traceContext: OperationTraceSpan
   ): Flow<Either<StreamErrorMessage, TypedInstance>> {
      val keyPattern = getRedisKeyPattern(unwrappedReturnType)
      val scanPattern = getScanPattern(keyPattern)

      logger.debug { "Starting Redis keyspace notifications for pattern: $scanPattern" }

      // Keyspace notification pattern: __keyspace@0__:pattern
      // For all keys, we listen to all events
      val notificationChannel = "__keyspace@0__:$scanPattern"

      traceContext.addEventNow(
         TracingEventKind.CACHE_REQUEST,
         TraceEventDirection.OUT,
         redisConnectionConfig.addresses.joinToString(),
         mapOf(
            "operation" to "PSUBSCRIBE",
            "pattern" to notificationChannel,
            "mode" to "streaming"
         )
      )

      val flowSink = MutableSharedFlow<Either<StreamErrorMessage, TypedInstance>>()
      val pubSubConnection = connection.statefulConnection.connectPubSub()
      val isActive = AtomicBoolean(true)

      val dataSource = DataSource.cache(
         redisConnectionConfig.connectionName,
         redisConnectionConfig.driverName,
         "KEYSPACE"
      )

      pubSubConnection.addListener(object : RedisPubSubAdapter<String, String>() {
         override fun message(pattern: String, channel: String, message: String) {
            if (!isActive.get()) return

            // Extract the key from the channel name (__keyspace@0__:key)
            val key = channel.substringAfter("__keyspace@0__:")

            // message contains the operation type: "set", "del", "expire", etc.
            // We're interested in "set" and "setex" events
            if (message in listOf("set", "setex", "hset")) {
               // Fetch the value
               val jsonValue = connection.sync().get(key)
               if (jsonValue != null) {
                  val typedInstanceResult = RedisJsonValueReader.toTypedInstance(
                     jsonValue,
                     unwrappedReturnType as ObjectType,
                     schema,
                     dataSource
                  )

                  runBlocking {
                     when (typedInstanceResult) {
                        is Either.Right -> flowSink.emit(typedInstanceResult.value.right())
                        is Either.Left -> flowSink.emit(typedInstanceResult.value)
                     }
                  }
               }
            }
         }
      })

      pubSubConnection.async().psubscribe(notificationChannel)

      return flowSink.asSharedFlow()
         .onCompletion {
            isActive.set(false)
            pubSubConnection.async().punsubscribe(notificationChannel)
            pubSubConnection.close()
            traceContext.complete(SpanState.SUCCESS)
            logger.debug { "Redis keyspace notification listener for pattern $notificationChannel closed" }
         }
   }
}

// Extension to get the underlying connection for creating pub/sub connections
private val StatefulRedisConnection<String, String>.statefulConnection: StatefulRedisConnection<String, String>
   get() = this
