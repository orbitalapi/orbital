package io.vyne.query.graph.operationInvocation

import io.vyne.models.TypedInstance
import io.vyne.query.ProfilerOperation
import io.vyne.schemas.Operation
import io.vyne.schemas.Parameter
import io.vyne.schemas.Service

class DataSourceAwareOperationInvoker: OperationInvoker {
   override fun canSupport(service: Service, operation: Operation): Boolean {
      TODO("Not yet implemented")
   }

   override fun invoke(service: Service, operation: Operation, parameters: List<Pair<Parameter, TypedInstance>>, profilerOperation: ProfilerOperation): TypedInstance {
      TODO("Not yet implemented")
   }
}
