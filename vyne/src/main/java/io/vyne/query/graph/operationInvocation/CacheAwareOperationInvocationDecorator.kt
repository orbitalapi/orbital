package io.vyne.query.graph.operationInvocation

import com.google.common.cache.CacheBuilder
import io.vyne.models.TypedInstance
import io.vyne.query.ProfilerOperation
import io.vyne.schemas.Operation
import io.vyne.schemas.Parameter
import io.vyne.schemas.Service
import io.vyne.utils.log

class CacheAwareOperationInvocationDecorator(private val invoker: OperationInvoker) : OperationInvoker {

   private val cachedErrors = CacheBuilder.newBuilder()
      .build<String, Exception>()
   private val cachedResults = CacheBuilder
      .newBuilder()
      .build<String, TypedInstance>()

   override fun canSupport(service: Service, operation: Operation): Boolean {
      return invoker.canSupport(service, operation)
   }

   override fun invoke(service: Service, operation: Operation, parameters: List<Pair<Parameter, TypedInstance>>, profilerOperation: ProfilerOperation): TypedInstance {
      val key = generateCacheKey(service, operation, parameters)
      val result = cachedResults.getIfPresent(key)

      if (result != null) {
         return result
      }

      val previousError = cachedErrors.getIfPresent(key)
      if (previousError != null) {
//         log().warn("Last attempt to invoke operation with key $key resulted in exception ${previousError::class.simpleName}  - ${previousError.message}.  Not attempting, and will rethrow error")
         throw previousError
      }

      val value = try {
         invoker.invoke(service, operation, parameters, profilerOperation)
      } catch (exception:Exception) {
         log().warn("Operation with cache key $key failed with exception ${exception::class.simpleName} ${exception.message}.  This operation with params will not be attempted again")
         cachedErrors.put(key,exception)
         throw exception
      }


      cachedResults.put(key, value)
      return value

   }

   private fun generateCacheKey(service: Service, operation: Operation, parameters: List<Pair<Parameter, TypedInstance>>): String {
      return """${service.name}:${operation.name}:${
         parameters.joinToString(",") { (param, instance) ->
            "${param.name}=${instance.value}"
         }
      }"""
   }
}
