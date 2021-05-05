package io.vyne.query.graph.operationInvocation

import com.google.common.cache.CacheBuilder
import io.vyne.models.TypedInstance
import io.vyne.query.QueryContextEventDispatcher
import io.vyne.schemas.Parameter
import io.vyne.schemas.RemoteOperation
import io.vyne.schemas.Service
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import mu.KotlinLogging
import java.util.*

private val logger = KotlinLogging.logger {}


class CacheAwareOperationInvocationDecorator(private val invoker: OperationInvoker) : OperationInvoker {

   private val cachedErrors = CacheBuilder.newBuilder()
      .build<String, Exception>()
   private val cachedResults = CacheBuilder
      .newBuilder()
      .build<String, LinkedList<TypedInstance>>()

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
      val key = generateCacheKey(service, operation, parameters)

      val result = cachedResults.getIfPresent(key)

      if (result != null) {
         return result.asFlow()
      }

      val previousError = cachedErrors.getIfPresent(key)
      if (previousError != null) {
         logger.warn { "Last attempt to invoke operation with key $key resulted in exception ${previousError::class.simpleName}  - ${previousError.message}.  Not attempting, and will rethrow error" }
         throw previousError
      }

      return try {
         val cacheResult = LinkedList<TypedInstance>()
         invoker.invoke(service, operation, parameters, eventDispatcher, queryId).onEach {
            cacheResult.add(it)
         }.onCompletion {
            cachedResults.put(key, cacheResult)
         }
      } catch (exception: Exception) {
         logger.warn { "Operation with cache key $key failed with exceptioCacheAwareOperationInvocationDecoratorn ${exception::class.simpleName} ${exception.message}.  This operation with params will not be attempted again" }
         cachedErrors.put(key, exception)
         throw exception
      }

   }

   private fun generateCacheKey(
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
