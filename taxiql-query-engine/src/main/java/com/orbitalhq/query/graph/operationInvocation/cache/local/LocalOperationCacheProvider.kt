package com.orbitalhq.query.graph.operationInvocation.cache.local

import com.google.common.cache.Cache
import com.google.common.cache.CacheBuilder
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.query.connectors.*
import com.orbitalhq.schemas.CachingStrategy
import com.orbitalhq.schemas.GlobalSharedCache
import com.orbitalhq.schemas.NamedCache
import com.orbitalhq.schemas.QueryScopedCache
import com.orbitalhq.utils.abbreviate
import mu.KotlinLogging
import reactor.core.publisher.Flux
import java.util.concurrent.ConcurrentHashMap

/**
 * Can build Operation Caches that are stored locally, in-process
 * (vs. remote - eg., in Redis or Hazelcast)
 */
class LocalOperationCacheProvider(
   private val actorCache: Cache<OperationCacheKey, CachingOperatorInvoker>,
   private val maxSize: Int
) :
   OperationCacheProvider {
   companion object {
      /**
       * Returns a default, short-lived, cache provider.
       * These caches live for as long as a query.
       * This is useful for testing, but for actual prod code,
       * we should be looking up the cache provider using the strategy
       * returned from parsing the query
       */
      fun default(): LocalOperationCacheProvider {
         return LocalOperationCacheProvider(LocalCacheProviderBuilder.newCache(), 10)
      }
   }

   val cacheSize: Long
      get() {
         return actorCache.size()
      }

   override fun getCachingInvoker(operationKey: OperationCacheKey, invoker: OperationInvoker): CachingOperatorInvoker {
      return actorCache.get(operationKey) {
         val map = ConcurrentHashMap<OperationCacheKey, Flux<TypedInstance>>()
         val mapLoader: CacheFetcher = { key: OperationCacheKey, invocationParams, loader -> map.getOrPut(key, loader) }
         CachingOperatorInvoker(operationKey, invoker, maxSize, mapLoader)
      }
   }

   override fun evict(operationKey: OperationCacheKey) {
      actorCache.invalidate(operationKey)
      actorCache.cleanUp()
   }


}


class LocalCacheProviderBuilder : OperationCacheProviderBuilder {
   companion object {
      private val logger = KotlinLogging.logger {}

      fun newCache(): Cache<OperationCacheName, CachingOperatorInvoker> {
         return CacheBuilder.newBuilder()
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

   override fun buildOperationCache(strategy: CachingStrategy, maxSize: Int): OperationCacheProvider {
      val cache = when (strategy) {
         is QueryScopedCache -> newCache()
         is GlobalSharedCache -> caches.getOrPut(globalCacheName) { newCache() }
         is NamedCache -> caches.getOrPut(strategy.name) {
            logger.info { "Creating new cache ${strategy.name}" }
            newCache()
         }
         else -> error("${strategy::class.simpleName} is not suppoerted by this builder")
      }
      return LocalOperationCacheProvider(cache, maxSize)
   }


}
