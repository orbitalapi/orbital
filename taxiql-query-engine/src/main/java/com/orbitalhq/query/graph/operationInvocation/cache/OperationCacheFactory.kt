package com.orbitalhq.query.graph.operationInvocation.cache

import com.orbitalhq.LocalOperationCacheConfiguration
import com.orbitalhq.query.connectors.CacheFactory
import com.orbitalhq.query.connectors.CachingInvokerProvider
import com.orbitalhq.query.connectors.OperationCacheProviderBuilder
import com.orbitalhq.query.graph.operationInvocation.cache.local.LocalCacheProviderBuilder
import com.orbitalhq.schemas.CachingStrategy
import java.time.Duration


/**
 * Builds operation caches.
 * Accepting a list of OperationCacheProviderBuilder allows injecting
 * advanced caching facilities (like using Hazelcast)
 */
class OperationCacheFactory(
   private val maxCachedOperations: Int = LocalOperationCacheConfiguration.DEFAULT_MAX_CACHED_OPERATIONS,
   private val cachedOperationTtl: Duration = LocalOperationCacheConfiguration.DEFAULT_MAX_DURATION,
   private val providers: List<OperationCacheProviderBuilder>
) : CacheFactory {

   companion object {
      fun default() = OperationCacheFactory(
         providers = listOf(LocalCacheProviderBuilder())
      )
   }

   override fun getOperationCache(strategy: CachingStrategy): CachingInvokerProvider {
      val provider = providers.firstOrNull { it.canBuild(strategy) }
         ?: error("Unable to build an OperationCacheProvider for strategy ${strategy::class.simpleName}")
      return provider.buildOperationCache(strategy, maxCachedOperations, cachedOperationTtl, this)
   }

}
