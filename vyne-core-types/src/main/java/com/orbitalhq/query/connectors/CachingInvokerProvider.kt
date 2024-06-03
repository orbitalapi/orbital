package com.orbitalhq.query.connectors

import com.orbitalhq.schemas.CachingStrategy
import com.orbitalhq.schemas.QualifiedName
import com.orbitalhq.schemas.ServiceName
import java.time.Duration

typealias CacheConnectionName = String
typealias CacheName = String
object CacheNames {
   val CACHE_PREFIX = "com.orbital.caching.Cache"
   val CACHE_READ_OPERATION_NAME = "READ"
   fun cacheServiceName(connectionName: CacheConnectionName) = "$CACHE_PREFIX.$connectionName"

   fun isCacheName(serviceName: QualifiedName) = serviceName.fullyQualifiedName.startsWith(CACHE_PREFIX)
   fun isCacheName(serviceName: ServiceName) = serviceName.startsWith(CACHE_PREFIX)

   fun cacheReadOperationName(operationName: String):String = "readCache_$operationName"
}
interface CachingInvokerProvider {
   fun getCachingInvoker(operationKey: OperationCacheKey, invoker: OperationInvoker): CachingOperatorInvoker
   fun evict(operationKey: OperationCacheKey)
}

/**
 * The top level in the caching infrastructure.
 * Generally defers to a collection of OperationCacheProviderBuilder's
 * to create a cache
 */
interface CacheFactory {
   fun getOperationCache(strategy: CachingStrategy): CachingInvokerProvider
}

/**
 * Returns an object that knows how to build something that will
 * either
 */
interface OperationCacheProviderBuilder {
   fun canBuild(strategy: CachingStrategy): Boolean

   fun buildOperationCache(
      strategy: CachingStrategy,
      maxCachedOperations: Int,
      cachedOperationTtl: Duration,
      /**
       * Accepts the cache factory, to allow caches to create other caches.
       * This lets remote caches create local caches that operate as level-one caches
       */
      cacheFactory: CacheFactory
   ): CachingInvokerProvider
}


/**
 * When using caches, we sometimes opt for "named" caches, to keep them partitioned,
 * so that content from one operation isn't shared with content from other operations
 */
typealias OperationCacheName = String

/**
 * The actual cache key (ie., operation name + parameters)
 * that is used to look up a previous invocation of an operation
 */
typealias OperationCacheKey = String
