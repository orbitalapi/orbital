package com.orbitalhq.query.graph.operationInvocation.cache

import com.orbitalhq.query.connectors.OperationCacheProvider
import com.orbitalhq.query.connectors.OperationCacheProviderBuilder
import com.orbitalhq.query.graph.operationInvocation.cache.local.LocalCacheProviderBuilder
import com.orbitalhq.schemas.CachingStrategy


/**
 * Builds operation caches.
 * Accepting a list of OperationCacheProviderBuilder allows injecting
 * advanced caching facilities (like using Hazelcast)
 */
class OperationCacheFactory(
   val maxResultRecordCount: Int = 10,
   private val providers: List<OperationCacheProviderBuilder>
) {

   companion object {
      fun default() = OperationCacheFactory(
         providers = listOf(LocalCacheProviderBuilder())
      )
   }

   fun getOperationCache(strategy: CachingStrategy): OperationCacheProvider {
      val provider = providers.firstOrNull { it.canBuild(strategy) }
         ?: error("Unable to build an OperationCacheProvider for strategy ${strategy::class.simpleName}")
      return provider.buildOperationCache(strategy, maxResultRecordCount)
   }

}
