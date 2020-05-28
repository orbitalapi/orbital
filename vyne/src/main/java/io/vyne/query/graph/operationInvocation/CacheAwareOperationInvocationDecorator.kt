package io.vyne.query.graph.operationInvocation

import com.google.common.cache.CacheBuilder
import io.vyne.models.TypedInstance
import io.vyne.query.ProfilerOperation
import io.vyne.schemas.Operation
import io.vyne.schemas.Parameter
import io.vyne.schemas.Service
import java.util.concurrent.TimeUnit

class CacheAwareOperationInvocationDecorator(private val invoker: OperationInvoker) : OperationInvoker {

   private val cache = CacheBuilder
      .newBuilder()
      .build<String, TypedInstance>()

   override fun canSupport(service: Service, operation: Operation): Boolean {
      return invoker.canSupport(service, operation)
   }

   override fun invoke(service: Service, operation: Operation, parameters: List<Pair<Parameter, TypedInstance>>, profilerOperation: ProfilerOperation): TypedInstance {
      val key = generateCacheKey(service, operation, parameters)
      val result = cache.getIfPresent(key)

      return if (result == null) {
         val value = invoker.invoke(service, operation, parameters, profilerOperation)

         cache.put(key, value)
         value
      } else {
         result
      }
   }

   private fun generateCacheKey(service: Service, operation: Operation, parameters: List<Pair<Parameter, TypedInstance>>): String {
      return """${service.name}:${operation.name}:${parameters.joinToString(",") { (param, instance) ->
         "${param.name}=${instance.value}"
      }}"""
   }
}
