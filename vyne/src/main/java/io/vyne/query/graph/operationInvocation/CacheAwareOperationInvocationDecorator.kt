package io.vyne.query.graph.operationInvocation

import com.google.common.cache.CacheBuilder
import io.vyne.models.TypedInstance
import io.vyne.query.ProfilerOperation
import io.vyne.schemas.Parameter
import io.vyne.schemas.RemoteOperation
import io.vyne.schemas.Service
import io.vyne.utils.log
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking

class CacheAwareOperationInvocationDecorator(private val invoker: OperationInvoker) : OperationInvoker {

   private val cachedErrors = CacheBuilder.newBuilder()
      .build<String, Exception>()
   private val cachedResults = CacheBuilder
      .newBuilder()
      .build<String, TypedInstance>()

   override fun canSupport(service: Service, operation: RemoteOperation): Boolean {
      return invoker.canSupport(service, operation)
   }

   override fun invoke(service: Service, operation: RemoteOperation, parameters: List<Pair<Parameter, TypedInstance>>, profilerOperation: ProfilerOperation, queryId: String?): Flow<TypedInstance> {
      val key = generateCacheKey(service, operation, parameters)

      val result = cachedResults.getIfPresent(key)

      if (result != null) {
         return flow { emit(result) }
      }

      val previousError = cachedErrors.getIfPresent(key)
      if (previousError != null) {
         log().warn("Last attempt to invoke operation with key $key resulted in exception ${previousError::class.simpleName}  - ${previousError.message}.  Not attempting, and will rethrow error")
         throw previousError
      }

      val value = try {
         invoker.invoke(service, operation, parameters, profilerOperation, queryId)
      } catch (exception:Exception) {
         log().warn("Operation with cache key $key failed with exception ${exception::class.simpleName} ${exception.message}.  This operation with params will not be attempted again")
         cachedErrors.put(key,exception)
         throw exception
      }

      return value

   }

   private fun generateCacheKey(service: Service, operation: RemoteOperation, parameters: List<Pair<Parameter, TypedInstance>>): String {
      return """${service.name}:${operation.name}:${
         parameters.joinToString(",") { (param, instance) ->
            "${param.name}=${instance.value}"
         }
      }"""
   }
}
