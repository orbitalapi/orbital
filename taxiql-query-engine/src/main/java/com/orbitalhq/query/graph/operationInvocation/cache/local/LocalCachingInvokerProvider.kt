package com.orbitalhq.query.graph.operationInvocation.cache.local

import com.google.common.base.Ticker
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

private typealias CacheTtlInMillis = Long

/**
 * Can build Operation Caches that are stored locally, in-process
 * (vs. remote - eg., in Redis or Hazelcast)
 */
class LocalCachingInvokerProvider(
   // MP: 8-Jan-25
   // We used to pass the actorCache (a guava cache) directly here.
   // But, we need to support differing TTL's per object, to allow things like
   // long-lived streaming queries that calls cached intermittently throughout the lifecycle of the stream.
   // So, we now pass a function that will create the cache, given the TTL
   // (which guava requires at the time of construction)
   private val actorCache: Cache<OperationCacheKey, Cache<Long, CachingOperatorInvoker>>,
   private val ticker: Ticker
//   private val actorCacheBuilder: (Duration) -> Cache<OperationCacheKey, CachingOperatorInvoker>,
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
      fun default(ticker: Ticker = Ticker.systemTicker()): LocalCachingInvokerProvider {
         return LocalCachingInvokerProvider(
            LocalCacheProviderBuilder.newCache(
               LocalOperationCacheConfiguration.DEFAULT_MAX_CACHED_OPERATIONS,
               ticker = ticker
            ),
            ticker = ticker
         )
      }
   }

   val cacheSize: Long
      get() {
         return actorCache.asMap()
            .values
            .sumOf { it.size() }
      }

   override fun getCachingInvoker(
      operationKey: OperationCacheKey,
      invoker: OperationInvoker,
      ttl: Duration
   ): CachingOperatorInvoker {
      // First, use the cache builder to retrieve or construct a cache, based on the TTL
      val invokersCachedWithTTL = actorCache.get(operationKey) {
         CacheBuilder.newBuilder()
            .ticker(ticker)
            .expireAfterAccess(ttl)
            .build()
      }
      val cachingInvoker = invokersCachedWithTTL.get(ttl.toMillis()) {
         DefaultCachingOperatorInvoker(operationKey, invoker, ttl, LocalCacheFetcher())
      }
      return cachingInvoker
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
   fun newLocalCache(): ReadCacheOrCallInvokerHandler {
      return LocalCacheFetcher()
   }
}

class LocalCacheFetcher : ReadCacheOrCallInvokerHandler {
   private val cachedFlux = ConcurrentHashMap<String, Flux<TypedInstance>>()
   override fun getCachedOrCallLoader(
      operationCacheKey: OperationCacheKey,
      operationInvocationParamMessage: OperationInvocationParamMessage,
      cacheTTL: Duration,
      invoker: () -> Flux<TypedInstance>
   ): Flux<TypedInstance> {
      return cachedFlux.getOrPut(operationCacheKey) {
         invoker().cache()
      }
   }
}

class LocalCacheProviderBuilder(private val ticker: Ticker) : OperationCacheProviderBuilder {
   companion object {
      private val logger = KotlinLogging.logger {}

      // We end up building a cache of caches.
      // This TTL defines how long the cache itself lives for -
      // not the individual records within the cache
      // It probably doesn't need to be configurable, and a long value is
      // probably fine.
      private val DEFAULT_OUTER_CACHE_TTL: Duration = Duration.ofHours(4)
      fun newCache(
         maxSize: Int,
         outerCacheTTL: Duration = DEFAULT_OUTER_CACHE_TTL,
         ticker: Ticker
      ): Cache<OperationCacheKey, Cache<CacheTtlInMillis, CachingOperatorInvoker>> {
         return CacheBuilder.newBuilder()
            .expireAfterAccess(outerCacheTTL)
            .maximumSize(maxSize.toLong())
            .ticker(ticker)
            .removalListener<OperationCacheName, Any> { notification ->
               logger.info { "Caching operation invoker removing entry for ${notification.key?.abbreviate()} for reason ${notification.cause}" }
            }
            .build()
      }
   }

   private val caches =
      ConcurrentHashMap<OperationCacheName, Cache<OperationCacheKey, Cache<CacheTtlInMillis, CachingOperatorInvoker>>>()
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
      cacheFactory: CacheFactory
   ): CachingInvokerProvider {
      val cacheOfCaches = when (strategy) {
         is QueryScopedCache -> newCache(maxCachedOperations, ticker = ticker)
         is GlobalSharedCache -> caches.getOrPut(globalCacheName) { newCache(maxCachedOperations, ticker = ticker) }
         is NamedCache -> caches.getOrPut(strategy.name) {
            logger.info { "Creating new cache ${strategy.name}" }
            newCache(maxCachedOperations, ticker = ticker)
         }

         else -> error("${strategy::class.simpleName} is not suppoerted by this builder")
      }
      return LocalCachingInvokerProvider(cacheOfCaches, ticker)
   }


}
