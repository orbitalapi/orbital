package com.orbitalhq.query.connectors

import com.orbitalhq.query.connectors.OperationInvoker
import com.orbitalhq.schemas.CachingStrategy
import com.orbitalhq.schemas.OperationName
import com.orbitalhq.schemas.OperationNames
import com.orbitalhq.schemas.QualifiedName
import com.orbitalhq.schemas.ServiceName

typealias CacheConnectionName = String
typealias CacheName = String
object CacheNames {
   val CACHE_PREFIX = "com.orbital.caching.Cache"
   val CACHE_READ_OPERATION_NAME = "READ"
   fun cacheServiceName(connectionName: CacheConnectionName) = "$CACHE_PREFIX.$connectionName"

   fun isCacheName(serviceName: QualifiedName) = serviceName.fullyQualifiedName.startsWith(CACHE_PREFIX)
   fun isCacheName(serviceName: ServiceName) = serviceName.startsWith(CACHE_PREFIX)
}
interface OperationCacheProvider {
   fun getCachingInvoker(operationKey: OperationCacheKey, invoker: OperationInvoker): CachingOperatorInvoker
   fun evict(operationKey: OperationCacheKey)
}
/**
 * Returns
 *
 * Responsibilities:
 *  - Factory : xxxx
 *  - OperationCacheBuilderStrategy :  xxxx
 *  - CachingInvocationActor: xxxx
 *  - LoadingCache<String,CachingInvocationActor>: xxxx
 */
interface OperationCacheProviderBuilder {
   fun canBuild(strategy: CachingStrategy): Boolean

   fun buildOperationCache(strategy: CachingStrategy, maxSize: Int): OperationCacheProvider
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
