package com.orbitalhq.connectors.hazelcast

import com.hazelcast.core.HazelcastInstance
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.query.connectors.CacheFactory
import com.orbitalhq.query.connectors.CachingOperatorInvoker
import com.orbitalhq.query.connectors.DefaultCachingOperatorInvoker
import com.orbitalhq.query.connectors.OperationCacheKey
import com.orbitalhq.query.connectors.CachingInvokerProvider
import com.orbitalhq.query.connectors.OperationCacheProviderBuilder
import com.orbitalhq.query.connectors.OperationInvocationParamMessage
import com.orbitalhq.query.connectors.OperationInvoker
import com.orbitalhq.query.connectors.ReadCacheOrCallInvokerHandler
import com.orbitalhq.query.graph.operationInvocation.cache.local.CascadingCacheInvoker
import com.orbitalhq.query.graph.operationInvocation.cache.local.LocalCache
import com.orbitalhq.schema.consumer.SchemaStore
import com.orbitalhq.schemas.CachingStrategy
import com.orbitalhq.schemas.RemoteCache
import mu.KotlinLogging
import reactor.core.publisher.Flux
import java.time.Clock
import java.time.Duration

private val logger = KotlinLogging.logger {}

class HazelcastCachingInvokerProvider(
   private val hazelcast: HazelcastInstance,
   private val schemaStore: SchemaStore,
   nearCacheMaxSize: Int = 1000,
   private val connectionName: String,
   private val connectionAddress: String,
   private val clock: Clock = Clock.systemUTC(),
   private val localCache: ReadCacheOrCallInvokerHandler
) : CachingInvokerProvider, ReadCacheOrCallInvokerHandler {

   init {
      Flux.from(schemaStore.schemaChanged)
         .subscribe {
//            logger.info { "Schema changed, so invalidating cache ${HazelcastMapCachingProvider.OPERATION_CACHE_NAME}" }
//            val map = hazelcast.getMap<String,Any>(HazelcastMapCachingProvider.OPERATION_CACHE_NAME)
//            map.clear()
//            logger.info { "Cache ${HazelcastMapCachingProvider.OPERATION_CACHE_NAME} cleared" }
         }
   }


   /**
    * Returns a caching invoker that first uses a near cache,
    * then defers to Hazelcast to load.
    */
   override fun getCachingInvoker(
      operationKey: OperationCacheKey,
      invoker: OperationInvoker,
      ttl: Duration
   ): CachingOperatorInvoker {
      return CascadingCacheInvoker(
         operationKey,
         localCache,
         getHazelcastCachingInvoker(operationKey, invoker, ttl),
         ttl
      )
   }

   /**
    * Returns only the hazelcast caching invoker.
    */
   fun getHazelcastCachingInvoker(
      operationKey: OperationCacheKey,
      invoker: OperationInvoker,
      ttl: Duration
   ) = DefaultCachingOperatorInvoker(
      operationKey, invoker, ttl, this
   )

   override fun getCachedOrCallLoader(
      operationCacheKey: OperationCacheKey,
      operationInvocationParamMessage: OperationInvocationParamMessage,
      cacheTTL: Duration,
      invoker: () -> Flux<TypedInstance>
   ): Flux<TypedInstance> {
      return HazelcastCacheProviderFactory
         .instance(hazelcast, schemaStore, connectionName, connectionAddress, clock, cacheTTL)
         .load(operationCacheKey, operationInvocationParamMessage, invoker)
   }

   override fun evict(operationKey: OperationCacheKey) {
      HazelcastCacheProviderFactory
         .instance(hazelcast, schemaStore, connectionName, connectionAddress, clock)
         .evict(operationKey)
   }
}

/**
 * Builds Hazelcast backed cache providers that are used for
 * caching results of operation calls.
 */
class HazelcastOperationCacheBuilder(
   private val hazelcastConnectionsManager: HazelcastConnectionsManager,
   private val schemaStore: SchemaStore,
) :
   OperationCacheProviderBuilder {
   override fun canBuild(strategy: CachingStrategy): Boolean {
      if (strategy !is RemoteCache) return false
      return hazelcastConnectionsManager.canProvideHazelcastInstance(strategy.connectionName)
   }

   /**
    * Called on the start of every query.
    * Returns a wrapper around Hazelcast, along with a near-cache using
    *
    */
   override fun buildOperationCache(
      strategy: CachingStrategy,
      maxCachedOperations: Int,
      cacheFactory: CacheFactory
   ): CachingInvokerProvider {
      require(strategy is RemoteCache) { "Only RemoteCache is supported, but got ${strategy::class.simpleName}" }
      val (client, config) = hazelcastConnectionsManager.hazelcastConnection(strategy.connectionName)
      return HazelcastCachingInvokerProvider(
         client,
         schemaStore,
         maxCachedOperations,
         config.connectionName,
         config.addresses.joinToString(","),
         localCache = LocalCache.newLocalCache()
      )
   }
}
