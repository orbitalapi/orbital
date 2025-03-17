package com.orbitalhq.query.graph.operationInvocation.cache.local

import arrow.core.Either
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.query.StreamErrorMessage
import com.orbitalhq.query.connectors.CachingOperatorInvoker
import com.orbitalhq.query.connectors.OperationCacheKey
import com.orbitalhq.query.connectors.OperationInvocationParamMessage
import com.orbitalhq.query.connectors.ReadCacheOrCallInvokerHandler
import mu.KotlinLogging
import reactor.core.publisher.Flux
import java.time.Duration

class CascadingCacheInvoker(
   private val cacheKey: OperationCacheKey,
   private val levelOneCache: ReadCacheOrCallInvokerHandler,
   private val levelTwoCache: CachingOperatorInvoker,
   private val ttl: Duration
) : CachingOperatorInvoker {
   companion object {
      private val logger = KotlinLogging.logger {}
   }

   override fun invoke(message: OperationInvocationParamMessage): Flux<Either<StreamErrorMessage, TypedInstance>> {
      return levelOneCache.getCachedOrCallLoader(cacheKey, message, ttl) {
         logger.debug { "L1 cache miss on key $cacheKey" }
         levelTwoCache.invoke(message)
      }
   }
}
