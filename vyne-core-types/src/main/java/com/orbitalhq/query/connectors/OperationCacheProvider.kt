package com.orbitalhq.query.connectors

import com.orbitalhq.query.connectors.OperationInvoker
import com.orbitalhq.schemas.CachingStrategy

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
