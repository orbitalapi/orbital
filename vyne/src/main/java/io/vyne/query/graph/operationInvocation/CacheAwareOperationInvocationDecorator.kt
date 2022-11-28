package io.vyne.query.graph.operationInvocation

import com.google.common.cache.CacheBuilder
import io.vyne.models.TypedInstance
import io.vyne.query.QueryContextEventDispatcher
import io.vyne.query.connectors.OperationInvoker
import io.vyne.schemas.Parameter
import io.vyne.schemas.RemoteOperation
import io.vyne.schemas.Service
import io.vyne.utils.StrategyPerformanceProfiler
import io.vyne.utils.abbreviate
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.actor
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.reactive.asFlow
import mu.KotlinLogging
import org.apache.commons.lang3.StringUtils
import reactor.core.publisher.Flux
import java.time.Duration
import java.time.Instant

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
   val evictWhenResultSizeExceeds: Int = 10
) :
   OperationInvoker {

   private val actorCache = CacheBuilder.newBuilder()
      .removalListener<String, CachingInvocationActor> { notification ->
         logger.info { "Caching operation invoker removing entry for ${notification.key?.abbreviate()} for reason ${notification.cause}" }
      }
      .build<String, CachingInvocationActor>()

   val cacheSize: Long
      get() {
         return actorCache.size()
      }

   override fun canSupport(service: Service, operation: RemoteOperation): Boolean {
      return invoker.canSupport(service, operation)
   }

   override suspend fun invoke(
      service: Service,
      operation: RemoteOperation,
      parameters: List<Pair<Parameter, TypedInstance>>,
      eventDispatcher: QueryContextEventDispatcher,
      queryId: String?
   ): Flow<TypedInstance> {
      val (key, params) = getCacheKeyAndParamMessage(service, operation, parameters, eventDispatcher, queryId)
      val actor = actorCache.get(key) {
         buildActor(key, evictWhenResultSizeExceeds)
      }

      // Optimisation:
      // If we definitely already have a result, we can skip the actor
      // phase, and just return the result.  This should be safe of race conditions, as
      // the value only ever goes from null -> !null (never back).
      // This dropped the mean time in this class significantly
      actor.result?.let {
         params.recordElapsed(true)
         return it.asFlow()
      }

      // We didn't have a result.  Defer to the actor, to ensure that
      // if multiple attempts to load from this cache at the same arrive, we only
      // want one to hit the cached service.  The actor takes care of this for us
      actor.send(params)

      try {
         var emittedRecords = 0
         var evictedFromCache = false
         return params.deferred.await()
            .onEach {
               emittedRecords++
               if (!evictedFromCache && emittedRecords > evictWhenResultSizeExceeds) {
                  // Some cache keys can be huge
                  logger.info { "Response from ${key.abbreviate()} has exceeded max cachable records ($evictWhenResultSizeExceeds) so is being removed from the cache.  Subsequent calls will hit the original service, not the cache" }
                  actorCache.invalidate(key)
                  actorCache.cleanUp()
                  evictedFromCache = true
               }
            }
      } catch (e: Exception) {
         throw e
      }
   }

   private fun buildActor(key: String, evictWhenResultSizeExceeds: Int): CachingInvocationActor {
      return CachingInvocationActor(key, invoker, evictWhenResultSizeExceeds)
   }

   companion object {
      private fun getCacheKeyAndParamMessage(
         service: Service,
         operation: RemoteOperation,
         parameters: List<Pair<Parameter, TypedInstance>>,
         eventDispatcher: QueryContextEventDispatcher,
         queryId: String?
      ): Pair<String, OperationInvocationParamMessage> {
         return generateCacheKey(service, operation, parameters) to
            OperationInvocationParamMessage(
               service, operation, parameters, eventDispatcher, queryId
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
 * A Kotlin Actor which ensures a single in-flight request through to the downstream invoker.
 * We use a different actor for each cacheKey (operation + params combination).
 */
@ExperimentalCoroutinesApi
@ObsoleteCoroutinesApi // At the time of writing, there's no alternative provided by Kotlin
private class CachingInvocationActor(
   private val cacheKey: String,
   private val invoker: OperationInvoker,
   private val evictWhenResultSizeExceeds: Int
) {

   // We use a Flux here, instead of a flow, as Fluxes have the concept of a shareable / replayable flux, which
   // also terminates.  A sharedFlow never terminates, so is not a suitable replacement.
   var result: Flux<TypedInstance>? = null
   val channel = CoroutineScope(Dispatchers.IO).actor<OperationInvocationParamMessage> {

      // This actor is configured to operate on a single message at a time.
      // We check to see if the result flux exists (indicating that the actor has performed work previously)
      // and if so, return the cached flux.   If not, we build the flux and return it.
      while (!channel.isClosedForReceive) {
         var wasFromCache = false // Used for telemetry
         val params = channel.receive()
         if (result == null) {
            logger.debug { "${cacheKey.abbreviate()} cache miss, loading from Operation Invoker" }
            result = invokeUnderlyingService(params)
            wasFromCache = false
         } else {
            logger.debug { "${cacheKey.abbreviate()} cache hit, replaying from cache" }
            wasFromCache = true
         }

         var firstMessageReceived = false
         params.deferred.complete(result!!
            .doOnEach {
               if (!firstMessageReceived) {
                  firstMessageReceived = true
                  params.recordElapsed(wasFromCache)
               }
            }
            // calling .asFlow() creates a new flow from the flux, which does complete. (Unlike a sharedFlow, which does not complete)
            .asFlow()
         )
      }
   }

   /**
    * Build a flux from the underlying OperationInvoker.
    */
   private suspend fun invokeUnderlyingService(message: OperationInvocationParamMessage): Flux<TypedInstance> {
      val (service: Service,
         operation: RemoteOperation,
         parameters: List<Pair<Parameter, TypedInstance>>,
         eventDispatcher: QueryContextEventDispatcher,
         queryId: String?) = message

      // A bit of async framework hopping here.
      // Invoker.invoke() is a suspend function, but we need to operate in a flux to allow
      // caching (not supported in Flow).
      // So we have to do our deferred flux work on the current coroutine context.
      val context = currentCoroutineContext()
      return Flux.create<TypedInstance> { sink ->
         CoroutineScope(context).async {
            try {
               invoker.invoke(service, operation, parameters, eventDispatcher, queryId)
                  .catch { exception ->
                     logger.info { "Operation with cache key ${cacheKey.abbreviate()} failed with exception ${exception::class.simpleName} ${exception.message}.  This operation with params will not be attempted again.  Future attempts will have this error replayed" }
                     sink.error(exception)
                  }
                  .onCompletion {
                     sink.complete()
                  }
                  .collect {
                     sink.next(it)
                  }
            } catch(exception:Exception) {
               // This is an exception thrown in the invoke method, but not within the flux / flow.
               // ie., something has gone wrong internally, not in the service.
               sink.error(exception)
            }
         }
         // Only cache up to the max size.  If we exceed this number, the Flux itself is removed from the cache, so not
         // presented for replay.
      }.cache(evictWhenResultSizeExceeds)

   }

   suspend fun send(message: OperationInvocationParamMessage) {
      this.channel.send(message)
   }
}

private data class OperationInvocationParamMessage(
   val service: Service,
   val operation: RemoteOperation,
   val parameters: List<Pair<Parameter, TypedInstance>>,
   val eventDispatcher: QueryContextEventDispatcher,
   val queryId: String?
) {
   fun recordElapsed(wasFromCache: Boolean) {
      if (wasFromCache) {
         StrategyPerformanceProfiler.record(
            "CacheAwareOperationInvocationDecorator.Load from cache",
            Duration.between(requestTime, Instant.now())
         )
      } else {
         StrategyPerformanceProfiler.record(
            "CacheAwareOperationInvocationDecorator.Load from remote service",
            Duration.between(requestTime, Instant.now())
         )
      }
   }

   val requestTime = Instant.now()
   val deferred = CompletableDeferred<Flow<TypedInstance>>()
}
