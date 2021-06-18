package io.vyne.query.graph.operationInvocation

import com.google.common.cache.CacheBuilder
import io.vyne.models.TypedInstance
import io.vyne.query.QueryContextEventDispatcher
import io.vyne.schemas.Parameter
import io.vyne.schemas.RemoteOperation
import io.vyne.schemas.Service
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.channels.actor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.reactive.asFlow
import mu.KotlinLogging
import reactor.core.publisher.Flux
import reactor.core.publisher.Sinks

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
class CacheAwareOperationInvocationDecorator(private val invoker: OperationInvoker) :
   OperationInvoker {

   private val actorCache = CacheBuilder.newBuilder()
      .build<String, CachingInvocationActor>()

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
         buildActor(key)
      }
      actor.send(params)
      try {
         return params.deferred.await()
      } catch (e: Exception) {
         throw e
      }
   }

   private fun buildActor(key: String): CachingInvocationActor {
      return CachingInvocationActor(key, invoker)
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
private class CachingInvocationActor(private val cacheKey: String, private val invoker: OperationInvoker) {

   // We use a Flux here, instead of a flow, as Fluxes have the concept of a shareable / replayable flux, which
   // also terminates.  A sharedFlow never terminates, so is not a suitable replacement.
   private var result: Flux<TypedInstance>? = null
   val channel = CoroutineScope(Dispatchers.IO).actor<OperationInvocationParamMessage> {
      while (!channel.isClosedForReceive) {
         val params = channel.receive()
         if (result == null) {
            logger.debug { "$cacheKey doing work" }
            result = handleMessage(params)
         }
         // calling .asFlow() creates a new flow from the flux, which does complete. (Unlike a sharedFlow, which does not complete)
         params.deferred.complete(result!!.asFlow())
      }
   }

   private suspend fun handleMessage(message: OperationInvocationParamMessage): Flux<TypedInstance> {
      val (service: Service,
         operation: RemoteOperation,
         parameters: List<Pair<Parameter, TypedInstance>>,
         eventDispatcher: QueryContextEventDispatcher,
         queryId: String?) = message
      val sink = Sinks.many().replay().all<TypedInstance>()
      try {
         invoker.invoke(service, operation, parameters, eventDispatcher, queryId)
            .catch { exception ->
               logger.info { "Operation with cache key $cacheKey failed with exception ${exception::class.simpleName} ${exception.message}.  This operation with params will not be attempted again.  Future attempts will have this error replayed" }
               val errorEmissionResult = sink.tryEmitError(exception)
               if (errorEmissionResult.isFailure) {
                  logger.error { "Failed to emit error on replayed flux.  This will cause downstream issues.  $errorEmissionResult" }
               }
            }
            .onCompletion {
               sink.tryEmitComplete()
            }
            .collect { sink.tryEmitNext(it) }

      } catch (exception: Exception) {
         logger.info { "Operation with cache key $cacheKey failed with exception ${exception::class.simpleName} ${exception.message}.  This operation with params will not be attempted again.  Future attempts will have this error replayed" }
         sink.tryEmitError(exception)
      }
      return sink.asFlux()

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
   val deferred = CompletableDeferred<Flow<TypedInstance>>()
}
