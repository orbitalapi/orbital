package io.osmosis.polymer.schemas

import io.osmosis.polymer.models.TypedValue


interface OperationInvoker {
   fun canSupport(service: Service, operation: Operation): Boolean
   // TODO : This should return some form of reactive type.
   fun invoke(operation: Operation, parameters: List<TypedValue>): TypedValue
}

object DefaultInvokers {
   val invokers = listOf(ToDoInvoker())
}

@Deprecated("For spiking purposes, will be removed")
class ToDoInvoker : OperationInvoker {
   override fun canSupport(service: Service, operation: Operation): Boolean = true

   override fun invoke(operation: Operation, parameters: List<TypedValue>): TypedValue {
      TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
   }

}

data class Parameter(val type: Type, val metadata: List<Metadata> = emptyList())

data class Operation(val name: String, val parameters: List<Parameter>, val returnType: Type, val metadata: List<Metadata> = emptyList())
data class Service(val qualifiedName: String, val operations: List<Operation>, val metadata: List<Metadata> = emptyList()) {
   fun operation(name: String): Operation {
      return this.operations.first { it.name == name }
   }
}

