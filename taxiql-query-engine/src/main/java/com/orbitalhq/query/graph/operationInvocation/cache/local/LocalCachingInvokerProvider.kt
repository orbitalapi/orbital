package com.orbitalhq.query.graph.operationInvocation.cache.local

import com.google.common.cache.Cache
import com.google.common.cache.CacheBuilder
import com.orbitalhq.LocalOperationCacheConfiguration
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.query.connectors.CacheFactory
import com.orbitalhq.query.connectors.ReadCacheOrCallInvokerHandler
import com.orbitalhq.query.connectors.CachingOperatorInvoker
import com.orbitalhq.query.connectors.DefaultCachingOperatorInvoker
import com.orbitalhq.query.connectors.OperationCacheKey
import com.orbitalhq.query.connectors.OperationCacheName
import com.orbitalhq.query.connectors.CachingInvokerProvider
import com.orbitalhq.query.connectors.OperationCacheProviderBuilder
import com.orbitalhq.query.connectors.OperationInvocationParamMessage
import com.orbitalhq.query.connectors.OperationInvoker
import com.orbitalhq.schemas.CachingStrategy
import com.orbitalhq.schemas.GlobalSharedCache
import com.orbitalhq.schemas.NamedCache
import com.orbitalhq.schemas.QueryScopedCache
import com.orbitalhq.utils.abbreviate
import mu.KotlinLogging
import reactor.core.publisher.Flux
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap

/**
 * Can build Operation Caches that are stored locally, in-process
 * (vs. remote - eg., in Redis or Hazelcast)
 */
class LocalCachingInvokerProvider(
   private val actorCache: Cache<OperationCacheKey, CachingOperatorInvoker>,
) :
   CachingInvokerProvider {
   companion object {
      /**
       * Returns a default, short-lived, cache provider.
       * These caches live for as long as a query.
       * This is useful for testing, but for actual prod code,
       * we should be looking up the cache provider using the strategy
       * returned from parsing the query
       */
      fun default(): LocalCachingInvokerProvider {
         return LocalCachingInvokerProvider(
            LocalCacheProviderBuilder.newCache(
               LocalOperationCacheConfiguration.DEFAULT_MAX_CACHED_OPERATIONS,
               LocalOperationCacheConfiguration.DEFAULT_MAX_DURATION
            )
         )
      }
   }

   val cacheSize: Long
      get() {
         return actorCache.size()
      }

   override fun getCachingInvoker(
      operationKey: OperationCacheKey,
      invoker: OperationInvoker
   ): CachingOperatorInvoker {
      return actorCache.get(operationKey) {
         DefaultCachingOperatorInvoker(operationKey, invoker, LocalCacheFetcher())
      }
   }

   override fun evict(operationKey: OperationCacheKey) {
      actorCache.invalidate(operationKey)
      actorCache.cleanUp()
   }
}

/**
 * Exposes a LocalCacheFetcher, used as L1 cache
 */
object LocalCache {
   fun newLocalCache():ReadCacheOrCallInvokerHandler {
      return LocalCacheFetcher()
   }
}
class LocalCacheFetcher : ReadCacheOrCallInvokerHandler {
   private val cachedFlux = ConcurrentHashMap<String, Flux<TypedInstance>>()
   override fun getCachedOrCallLoader(
      operationCacheKey: OperationCacheKey,
      operationInvocationParamMessage: OperationInvocationParamMessage,
      invoker: () -> Flux<TypedInstance>
   ): Flux<TypedInstance> {
      return cachedFlux.getOrPut(operationCacheKey) {
         invoker().cache()
      }
   }
}

class LocalCacheProviderBuilder : OperationCacheProviderBuilder {
   companion object {
      private val logger = KotlinLogging.logger {}

      fun newCache(maxSize: Int, maxDuration: Duration): Cache<OperationCacheName, CachingOperatorInvoker> {
         return CacheBuilder.newBuilder()
            .expireAfterAccess(maxDuration)
            .maximumSize(maxSize.toLong())
            .removalListener<String, CachingOperatorInvoker> { notification ->
               logger.info { "Caching operation invoker removing entry for ${notification.key?.abbreviate()} for reason ${notification.cause}" }
            }
            .build()
      }
   }

   private val caches = ConcurrentHashMap<OperationCacheName, Cache<OperationCacheKey, CachingOperatorInvoker>>()
   private val globalCacheName = "GLOBAL"

   override fun canBuild(strategy: CachingStrategy): Boolean = when (strategy) {
      is QueryScopedCache,
      is GlobalSharedCache,
      is NamedCache -> true

      else -> false
   }

   override fun buildOperationCache(
      strategy: CachingStrategy,
      maxCachedOperations: Int,
      cachedOperationTtl: Duration,
      cacheFactory: CacheFactory
   ): CachingInvokerProvider {
      val cache = when (strategy) {
         is QueryScopedCache -> newCache(maxCachedOperations, cachedOperationTtl)
         is GlobalSharedCache -> caches.getOrPut(globalCacheName) { newCache(maxCachedOperations, cachedOperationTtl) }
         is NamedCache -> caches.getOrPut(strategy.name) {
            logger.info { "Creating new cache ${strategy.name}" }
            newCache(maxCachedOperations, cachedOperationTtl)
         }

         else -> error("${strategy::class.simpleName} is not suppoerted by this builder")
      }
      return LocalCachingInvokerProvider(cache)
   }


}
