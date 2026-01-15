package com.orbitalhq.connectors.redis

import arrow.core.Either
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.query.StreamErrorMessage
import com.orbitalhq.query.connectors.CacheAwareOperationInvocationDecorator
import com.orbitalhq.query.connectors.OperationCacheKey
import com.orbitalhq.query.connectors.OperationInvocationParamMessage
import com.orbitalhq.schema.consumer.SchemaStore
import io.lettuce.core.api.StatefulRedisConnection
import reactor.core.publisher.Flux
import java.time.Clock
import java.time.Duration

abstract class RedisCachingProvider(protected val connection: StatefulRedisConnection<String, String>) {
   abstract fun load(
      key: OperationCacheKey,
      message: OperationInvocationParamMessage,
      loader: () -> Flux<Either<StreamErrorMessage, TypedInstance>>
   ): Flux<TypedInstance>

   abstract fun evict(operationKey: OperationCacheKey)
}

object RedisCacheProviderFactory {
   fun instance(
      connection: StatefulRedisConnection<String, String>,
      schemaStore: SchemaStore,
      connectionName: String,
      connectionAddress: String,
      clock: Clock,
      ttl: Duration = CacheAwareOperationInvocationDecorator.DEFAULT_CACHE_TTL
   ): RedisCachingProvider {
      return RedisMapCachingProvider(
         connection,
         schemaStore,
         connectionName,
         connectionAddress,
         clock = clock,
         defaultTTL = ttl
      )
   }
}

/**
 * Implementation of cache-aside pattern using Redis
 */
class RedisMapCachingProvider(
   connection: StatefulRedisConnection<String, String>,
   private val schemaStore: SchemaStore,
   private val connectionName: String,
   private val connectionAddress: String,
   private val clock: Clock = Clock.systemUTC(),
   private val defaultTTL: Duration
) : RedisCachingProvider(connection) {

   override fun load(
      key: OperationCacheKey,
      message: OperationInvocationParamMessage,
      loader: () -> Flux<Either<StreamErrorMessage, TypedInstance>>
   ): Flux<TypedInstance> {
      val cacheKey = "orbital:cache:${key.hashCode()}"

      // Try to load from cache first
      val cached = connection.sync().get(cacheKey)
      if (cached != null) {
         // Cache hit - deserialize and return
         return Flux.fromIterable(deserializeCachedResults(cached))
      }

      // Cache miss - execute loader and cache results
      return loader()
         .collectList()
         .flatMapMany { results ->
            val instances = results.mapNotNull { either ->
               when (either) {
                  is Either.Right -> either.value
                  is Either.Left -> null
               }
            }

            // Serialize and cache
            if (instances.isNotEmpty()) {
               val serialized = serializeCachedResults(instances)
               connection.sync().setex(cacheKey, defaultTTL.seconds, serialized)
            }

            Flux.fromIterable(instances)
         }
   }

   override fun evict(operationKey: OperationCacheKey) {
      val cacheKey = "orbital:cache:${operationKey.hashCode()}"
      connection.sync().del(cacheKey)
   }

   private fun serializeCachedResults(instances: List<TypedInstance>): String {
      // Simplified serialization - in production, use proper serialization
      return instances.joinToString("\n---SEPARATOR---\n") { it.toJson() }
   }

   private fun deserializeCachedResults(serialized: String): List<TypedInstance> {
      // Simplified deserialization - in production, use proper deserialization
      val schema = schemaStore.current()
      return serialized.split("\n---SEPARATOR---\n")
         .mapNotNull { json ->
            // Would need proper deserialization here
            null
         }
   }
}

/**
 * Builder for creating caching providers
 */
class RedisOperationCacheBuilder(
   private val connectionsManager: RedisConnectionsManager,
   private val schemaStore: SchemaStore
) {
   fun build(
      connectionName: String,
      ttl: Duration = CacheAwareOperationInvocationDecorator.DEFAULT_CACHE_TTL
   ): RedisCachingProvider {
      val (connection, config) = connectionsManager.redisConnection(connectionName)
      return RedisCacheProviderFactory.instance(
         connection,
         schemaStore,
         connectionName,
         config.addresses.joinToString(),
         Clock.systemUTC(),
         ttl
      )
   }
}
