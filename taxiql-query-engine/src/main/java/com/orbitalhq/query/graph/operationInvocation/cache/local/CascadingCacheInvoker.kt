package com.orbitalhq.query.graph.operationInvocation.cache.local

import com.orbitalhq.models.TypedInstance
import com.orbitalhq.query.connectors.ReadCacheOrCallInvokerHandler
import com.orbitalhq.query.connectors.CachingOperatorInvoker
import com.orbitalhq.query.connectors.OperationCacheKey
import com.orbitalhq.query.connectors.OperationInvocationParamMessage
import mu.KotlinLogging
import reactor.core.publisher.Flux

class CascadingCacheInvoker(
   private val cacheKey: OperationCacheKey,
   private val levelOneCache: ReadCacheOrCallInvokerHandler,
   private val levelTwoCache: CachingOperatorInvoker,
) : CachingOperatorInvoker {
   companion object {
      private val logger = KotlinLogging.logger {}
   }

   override fun invoke(message: OperationInvocationParamMessage): Flux<TypedInstance> {
      return levelOneCache.getCachedOrCallLoader(cacheKey, message) {
         logger.debug { "L1 cache miss on key $cacheKey" }
         levelTwoCache.invoke(message)
      }
   }
}
