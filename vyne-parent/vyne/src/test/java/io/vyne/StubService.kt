package io.vyne

import io.vyne.models.TypedInstance
import io.vyne.query.ProfilerOperation
import io.vyne.query.graph.operationInvocation.OperationInvoker
import io.vyne.schemas.Operation
import io.vyne.schemas.Parameter
import io.vyne.schemas.Service
import io.vyne.utils.orElse

class StubService(val responses: MutableMap<String, TypedInstance> = mutableMapOf()) : OperationInvoker {
   constructor(vararg responses: Pair<String, TypedInstance>) : this(responses.toMap().toMutableMap())

   val invocations = mutableMapOf<String, List<TypedInstance>>()

   override fun invoke(service: Service, operation: Operation, parameters: List<Pair<Parameter,TypedInstance>>, profiler: ProfilerOperation): TypedInstance {
      val stubResponseKey = if (operation.hasMetadata("StubResponse")) {
         val metadata = operation.metadata("StubResponse")
         (metadata.params["value"] as String?).orElse(operation.name)
      } else {
         operation.name
      }
      invocations.put(stubResponseKey, parameters.map { it.second })

      if (!responses.containsKey(stubResponseKey)) {
         throw IllegalArgumentException("No stub response prepared for operation $stubResponseKey")
      }
      return responses[stubResponseKey]!!
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
      return operation.metadata.any { it.name.name == "StubResponse" } || this.responses.containsKey(operation.name)
   }


}
