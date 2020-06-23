package io.vyne

import io.vyne.models.TypedInstance
import io.vyne.query.ProfilerOperation
import io.vyne.query.graph.operationInvocation.DefaultOperationInvocationService
import io.vyne.query.graph.operationInvocation.OperationInvocationService
import io.vyne.query.graph.operationInvocation.OperationInvoker
import io.vyne.schemas.Operation
import io.vyne.schemas.Parameter
import io.vyne.schemas.Service
import io.vyne.utils.log
import io.vyne.utils.orElse

typealias StubResponseHandler = (Operation, List<Pair<Parameter, TypedInstance>>) -> TypedInstance

class StubService(val responses: MutableMap<String, TypedInstance> = mutableMapOf(), val handlers: MutableMap<String, StubResponseHandler> = mutableMapOf()) : OperationInvoker {
   constructor(vararg responses: Pair<String, TypedInstance>) : this(responses.toMap().toMutableMap())

   fun toOperationInvocationService():OperationInvocationService {
      return DefaultOperationInvocationService(
         listOf(this)
      )
   }
   val invocations = mutableMapOf<String, List<TypedInstance>>()

   override fun invoke(service: Service, operation: Operation, parameters: List<Pair<Parameter, TypedInstance>>, profiler: ProfilerOperation): TypedInstance {
      log().info("Invoking ${service.name} -> ${operation.name}(${parameters.map { it.first.name }})")
      val stubResponseKey = if (operation.hasMetadata("StubResponse")) {
         val metadata = operation.metadata("StubResponse")
         (metadata.params["value"] as String?).orElse(operation.name)
      } else {
         operation.name
      }
      invocations.put(stubResponseKey, parameters.map { it.second })

      if (!responses.containsKey(stubResponseKey) && !handlers.containsKey(stubResponseKey)) {
         throw IllegalArgumentException("No stub response or handler prepared for operation $stubResponseKey")
      }

      return if (responses.containsKey(stubResponseKey)) {
         responses[stubResponseKey]!!
      } else {
         handlers[stubResponseKey]!!.invoke(operation, parameters)
      }
   }

   fun addResponse(stubOperationKey: String, handler: StubResponseHandler): StubService {
      this.handlers.put(stubOperationKey, handler)
      return this;
   }

   fun addResponse(stubOperationKey: String, response: TypedInstance): StubService {
      this.responses.put(stubOperationKey, response)
      return this
   }

   override fun canSupport(service: Service, operation: Operation): Boolean {
      // TODO : why did I need to do this?
      // This was working by using annotations on the method to indicate that there was a stub response
      // Why do I care about that?  I could just use the method name as a default?!
      // I've changed this to look for a match with responses against the method name - revert if that turns out to be dumb.
      return operation.metadata.any { it.name.name == "StubResponse" } || this.responses.containsKey(operation.name) || this.handlers.containsKey(operation.name)
   }


}
