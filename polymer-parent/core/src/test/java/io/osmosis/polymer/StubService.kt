package io.osmosis.polymer

import io.osmosis.polymer.models.TypedInstance
import io.osmosis.polymer.query.graph.operationInvocation.OperationInvoker
import io.osmosis.polymer.schemas.Operation
import io.osmosis.polymer.schemas.Service

class StubService(val responses: MutableMap<String, TypedInstance> = mutableMapOf()) : OperationInvoker {
   override fun invoke(operation: Operation, parameters: List<TypedInstance>): TypedInstance {
      val metadata = operation.metadata("StubResponse")
      val stubResponseKey = metadata.params["value"]
      return responses[stubResponseKey]!!
   }

   fun addResponse(stubOperationKey: String, response: TypedInstance): StubService {
      this.responses.put(stubOperationKey, response)
      return this
   }

   override fun canSupport(service: Service, operation: Operation): Boolean {
      return operation.metadata.any { it.name.name == "StubResponse" }
   }


}
