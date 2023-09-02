package com.orbitalhq.query.graph.operationInvocation

import com.google.common.cache.Cache
import com.google.common.cache.CacheBuilder
import com.orbitalhq.schemas.CachingStrategy
import com.orbitalhq.schemas.GlobalSharedCache
import com.orbitalhq.schemas.NamedCache
import com.orbitalhq.schemas.QueryScopedCache
import com.orbitalhq.utils.abbreviate
import mu.KotlinLogging
import java.util.concurrent.ConcurrentHashMap

class OperationCacheFactory {

   companion object {
      private val logger = KotlinLogging.logger {}
      private val globalCacheName = "GLOBAL"

      fun newCache(): Cache<String, CachingInvocationActor> {
         return CacheBuilder.newBuilder()
            .removalListener<String, CachingInvocationActor> { notification ->
               logger.info { "Caching operation invoker removing entry for ${notification.key?.abbreviate()} for reason ${notification.cause}" }
            }
            .build<String, CachingInvocationActor>()
      }
   }

   private val caches = ConcurrentHashMap<String, Cache<String, CachingInvocationActor>>()
   fun getCache(strategy: CachingStrategy): Cache<String, CachingInvocationActor> {
      return when (strategy) {
         is QueryScopedCache -> newCache()
         is GlobalSharedCache -> caches.getOrPut(globalCacheName) { newCache() }
         is NamedCache -> caches.getOrPut(strategy.name) {
            logger.info { "Creating new cache ${strategy.name}" }
            newCache()
         }
      }
   }
}
