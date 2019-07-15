package io.vyne.regression

import io.vyne.models.TypedInstance
import io.vyne.query.ProfilerOperation
import io.vyne.query.RemoteCall
import io.vyne.query.graph.operationInvocation.OperationInvoker
import io.vyne.schemas.Operation
import io.vyne.schemas.Parameter
import io.vyne.schemas.Schema
import io.vyne.schemas.Service

class ReplayingOperationInvoker(private val remoteCalls: List<RemoteCall>, private val schema:Schema) : OperationInvoker {
   override fun canSupport(service: Service, operation: Operation): Boolean {
      // TODO : We might wanna consider strict ordering when replaying.
      return findRecordedCall(operation) != null
   }

   override fun invoke(service: Service, operation: Operation, parameters: List<Pair<Parameter, TypedInstance>>, profilerOperation: ProfilerOperation): TypedInstance {
      val recordedCall = findRecordedCall(operation) ?: error("Expected a matching recorded call")
      // TODO : Handle mulitple calls to the same operation with different params
      val responseType = schema.type(recordedCall.responseTypeName)
      return TypedInstance.from(responseType,recordedCall.response, schema)
   }

   private fun findRecordedCall(operation:Operation):RemoteCall? {
      return this.remoteCalls.firstOrNull { it.operationQualifiedName == operation.qualifiedName }
   }

}
