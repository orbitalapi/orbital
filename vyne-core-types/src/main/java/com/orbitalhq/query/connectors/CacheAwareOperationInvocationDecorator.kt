package com.orbitalhq.query.connectors

import com.orbitalhq.models.TypedInstance
import com.orbitalhq.query.QueryContextEventDispatcher
import com.orbitalhq.schemas.Parameter
import com.orbitalhq.schemas.QueryOptions
import com.orbitalhq.schemas.RemoteOperation
import com.orbitalhq.schemas.Service
import com.orbitalhq.utils.abbreviate
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactor.asFlux
import kotlinx.coroutines.runBlocking
import lang.taxi.services.OperationScope
import mu.KotlinLogging
import reactor.core.publisher.Flux

private val logger = KotlinLogging.logger {}


/**
 * Decorates an underlying invoker to cache and replay subsequent calls to the same service.
 *
 * Calls are cached against their operation and parameters, so a subsequent call to the same
 * operation with different params will miss the cache and continue through to the underlying invoker.
 *
 * Exceptions from the downstream invoker are replayed in subsequent cache hits.
 *
 * When multiple calls are received in parallel for the same operation, only one is executed, and
 * other concurrent requests wait until the result is received.
 *
 */
class CacheAwareOperationInvocationDecorator(
   private val invoker: OperationInvoker,
   private val cacheProvider: OperationCacheProvider,
   /**
    * Defines the max size (in result rows) of a result that can be cached.
    * For example - a database query that returns more rows than this will not
    * be cached (and will have it's existing cache records evicted)
    *
    * Is not related to the max number of invocations to cache.
    */
   private val evictWhenResultSizeExceeds: Int = 10
) :
   OperationInvoker {

   override fun canSupport(service: Service, operation: RemoteOperation): Boolean {
      return invoker.canSupport(service, operation)
   }

   override suspend fun invoke(
      service: Service,
      operation: RemoteOperation,
      parameters: List<Pair<Parameter, TypedInstance>>,
      eventDispatcher: QueryContextEventDispatcher,
      queryId: String,
      queryOptions: QueryOptions
   ): Flow<TypedInstance> {
      if (!isCacheable(operation, service, invoker)) {
         return invoker.invoke(service, operation, parameters, eventDispatcher, queryId, queryOptions)
      }

      val (key, params) = getCacheKeyAndParamMessage(service, operation, parameters, eventDispatcher, queryId, queryOptions)

      val cachingInvoker = cacheProvider.getCachingInvoker(key, invoker)
      var emittedRecords = 0
      var evictedFromCache = false

      // We didn't have a result.  Defer to the actor, to ensure that
      // if multiple attempts to load from this cache at the same arrive, we only
      // want one to hit the cached service.  The actor takes care of this for us
      val flux = cachingInvoker.invoke(params)
         .doOnNext {
            emittedRecords++
            if (!evictedFromCache && emittedRecords > evictWhenResultSizeExceeds) {
               // Some cache keys can be huge
               logger.info { "Response from ${key.abbreviate()} has exceeded max cachable records ($evictWhenResultSizeExceeds) so is being removed from the cache.  Subsequent calls will hit the original service, not the cache" }
               cacheProvider.evict(key)
               evictedFromCache = true
            }
         }

      return flux.asFlow()
   }

   private fun isCacheable(operation: RemoteOperation, service: Service, invoker: OperationInvoker): Boolean {
      // TODO : Make this richer
      return when {
         invoker.getCachingBehaviour(service, operation) == OperationCachingBehaviour.NO_CACHE -> false
         operation.operationType == OperationScope.MUTATION -> false
         operation.returnType.isStream -> false
         else -> true
      }
   }


   companion object {
      fun decorateAll(
         invokers: List<OperationInvoker>,
         evictWhenResultSizeExceeds: Int = 10,
         cacheProvider: OperationCacheProvider
      ): List<OperationInvoker> {
         return invokers.map { CacheAwareOperationInvocationDecorator(it, cacheProvider, evictWhenResultSizeExceeds) }
      }

      private fun getCacheKeyAndParamMessage(
         service: Service,
         operation: RemoteOperation,
         parameters: List<Pair<Parameter, TypedInstance>>,
         eventDispatcher: QueryContextEventDispatcher,
         queryId: String,
         queryOptions: QueryOptions
      ): Pair<String, OperationInvocationParamMessage> {
         return generateCacheKey(service, operation, parameters) to
            OperationInvocationParamMessage(
               service, operation, parameters, eventDispatcher, queryId, queryOptions
            )
      }

      fun generateCacheKey(
         service: Service,
         operation: RemoteOperation,
         parameters: List<Pair<Parameter, TypedInstance>>
      ): String {
         return """${service.name}:${operation.name}:${
            parameters.joinToString(",") { (param, instance) ->
               "${param.name}=${instance.value}"
            }
         }"""
      }
   }
}

/**
 * Takes two parameters:
 *  - The Cache Key (For looking up against some backing store - eg: a Concurrent Hash Map_
 *  - A loader function, which can be called to invoke the underlying service
 */
typealias CacheFetcher = (OperationCacheKey, OperationInvocationParamMessage, () -> Flux<TypedInstance>) -> Flux<TypedInstance>

/**
 * A Kotlin Actor which ensures a single in-flight request through to the downstream invoker.
 * We use a different actor for each cacheKey (operation + params combination).
 */
@ExperimentalCoroutinesApi
@ObsoleteCoroutinesApi // At the time of writing, there's no alternative provided by Kotlin
class CachingOperatorInvoker(
   private val cacheKey: OperationCacheKey,
   private val invoker: OperationInvoker,
   private val cacheFetcher: CacheFetcher
) {
   suspend fun invoke(message: OperationInvocationParamMessage): Flux<TypedInstance> {
      return cacheFetcher.invoke(cacheKey, message) {
         logger.debug { "${cacheKey.abbreviate()} cache miss, loading from Operation Invoker" }
         invokeUnderlyingService(message)
      }
   }

   /**
    * Build a flux from the underlying OperationInvoker.
    */
   private fun invokeUnderlyingService(message: OperationInvocationParamMessage): Flux<TypedInstance> {
      val (service: Service,
         operation: RemoteOperation,
         parameters: List<Pair<Parameter, TypedInstance>>,
         eventDispatcher: QueryContextEventDispatcher,
         queryId: String,
         queryOptions: QueryOptions) = message

      // A bit of async framework hopping here.
      // Invoker.invoke() is a suspend function, but we need to operate in a flux to allow
      // caching (not supported in Flow).
      // So we have to do our deferred flux work on the current coroutine context.
//      val context = currentCoroutineContext()

      return Flux.create<TypedInstance> { sink ->
         // This isn't really blocking anything. We just didn't understand how suspend / flux functions
         // worked when we wrote the underlying interface.
         runBlocking {
            try {
               invoker.invoke(service, operation, parameters, eventDispatcher, queryId, queryOptions)
                  .asFlux()
                  .doOnError {exception ->
                     logger.info { "Operation with cache key ${cacheKey.abbreviate()} failed with exception ${exception::class.simpleName} ${exception.message}.  This operation with params will not be attempted again.  Future attempts will have this error replayed" }
                     sink.error(exception)
                  }
                  .doOnComplete {
                     sink.complete()
                  }
                  .subscribe {
                     sink.next(it)
                  }
            } catch (exception: Exception) {
               logger.error(exception) { "An exception was thrown inside the invoker (${invoker::class.simpleName} calling ${operation.name})" }
               // This is an exception thrown in the invoke method, but not within the flux / flow.
               // ie., something has gone wrong internally, not in the service.
               sink.error(exception)
            }
         }
      }.share()


   }


}

data class OperationInvocationParamMessage(
   val service: Service,
   val operation: RemoteOperation,
   val parameters: List<Pair<Parameter, TypedInstance>>,
   val eventDispatcher: QueryContextEventDispatcher,
   val queryId: String,
   val queryOptions: QueryOptions
) {
}
