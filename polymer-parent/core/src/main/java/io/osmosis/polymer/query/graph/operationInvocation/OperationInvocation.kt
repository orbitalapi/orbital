package io.osmosis.polymer.query.graph.operationInvocation

import io.osmosis.polymer.models.TypedInstance
import io.osmosis.polymer.models.TypedValue
import io.osmosis.polymer.query.QueryContext
import io.osmosis.polymer.query.graph.EvaluatedLink
import io.osmosis.polymer.query.graph.LinkEvaluator
import io.osmosis.polymer.schemas.Link
import io.osmosis.polymer.schemas.Operation
import io.osmosis.polymer.schemas.Relationship
import io.osmosis.polymer.schemas.Service
import org.springframework.stereotype.Component

interface OperationInvoker {
   fun canSupport(service: Service, operation: Operation): Boolean
   // TODO : This should return some form of reactive type.
   fun invoke(operation: Operation, parameters: List<TypedInstance>): TypedInstance
}

object DefaultInvokers {
   val invokers = listOf(ToDoInvoker())
}

@Component
@Deprecated("For spiking purposes, will be removed")
class ToDoInvoker : OperationInvoker {
   override fun canSupport(service: Service, operation: Operation): Boolean = true

   override fun invoke(operation: Operation, parameters: List<TypedInstance>): TypedValue {
      TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
   }
}

@Component
class OperationInvocationEvaluator(val invokers: List<OperationInvoker>) : LinkEvaluator {
   override val relationship: Relationship = Relationship.PROVIDES

   override fun evaluate(link: Link, startingPoint: TypedInstance, context: QueryContext): EvaluatedLink {
      val operationName = link.start
      val (service, operation) = context.schema.operation(operationName)
      val invoker = invokers.firstOrNull { it.canSupport(service, operation) } ?: throw IllegalArgumentException("No invokers found for operation ${operationName.fullyQualifiedName}")

      // TODO : Need to gather the other args.  This is pretty lazy...
      val result: TypedInstance = invoker.invoke(operation, listOf(startingPoint))
      return EvaluatedLink(link, startingPoint, result)
   }
}
