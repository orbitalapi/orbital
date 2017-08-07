package io.osmosis.polymer.query.graph.operationInvocation

import io.osmosis.polymer.models.TypedInstance
import io.osmosis.polymer.models.TypedValue
import io.osmosis.polymer.query.QueryContext
import io.osmosis.polymer.query.QueryResult
import io.osmosis.polymer.query.QuerySpecTypeNode
import io.osmosis.polymer.query.graph.EvaluatedLink
import io.osmosis.polymer.query.graph.LinkEvaluator
import io.osmosis.polymer.schemas.*
import io.osmosis.polymer.utils.log
import org.springframework.stereotype.Component

/**
 * The parent to OperationInvokers
 */
interface OperationInvocationService {
   fun invokeOperation(service: Service, operation: Operation, preferredParams: Set<TypedInstance>, context: QueryContext): TypedInstance
}

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
class OperationInvocationEvaluator(val invokers: List<OperationInvoker>, private val constraintViolationResolver: ConstraintViolationResolver = ConstraintViolationResolver()) : LinkEvaluator, OperationInvocationService {
   override val relationship: Relationship = Relationship.PROVIDES

   override fun evaluate(link: Link, startingPoint: TypedInstance, context: QueryContext): EvaluatedLink {
      val operationName = link.start
      val (service, operation) = context.schema.operation(operationName)
      val result: TypedInstance = invokeOperation(service, operation, setOf(startingPoint), context)
      return EvaluatedLink(link, startingPoint, result)
   }

   override fun invokeOperation(service: Service, operation: Operation, preferredParams: Set<TypedInstance>, context: QueryContext): TypedInstance {
      val invoker = invokers.firstOrNull { it.canSupport(service, operation) } ?: throw IllegalArgumentException("No invokers found for operation ${operation.name}")

      val parameters = gatherParameters(operation.parameters, preferredParams, context)
      val resolvedParams = ensureParametersSatisfyContracts(operation.parameters, parameters, context)
      val result: TypedInstance = invoker.invoke(operation, resolvedParams)
      return result
   }

   private fun gatherParameters(parameters: List<Parameter>, preferredParams: Set<TypedInstance>, context: QueryContext): List<TypedInstance> {
      val preferredParamsByType = preferredParams.associateBy { it.type }
      val unresolvedParams = mutableListOf<QuerySpecTypeNode>()
      // Holds EITHER the param value, or a QuerySpecTypeNode which can be used
      // to query the engine for a value.
      val parameterValuesOrQuerySpecs: List<Any> = parameters.map { requiredParam ->
         when {
            preferredParamsByType.containsKey(requiredParam.type) -> preferredParamsByType[requiredParam.type]!!
            context.hasFactOfType(requiredParam.type) -> context.getFact(requiredParam.type)
            else -> {
               val queryNode = QuerySpecTypeNode(requiredParam.type)
               unresolvedParams.add(queryNode)
               queryNode
            }
         }
      }

      // Try to resolve any unresolved params
      var resolvedParams = emptyMap<QuerySpecTypeNode, TypedInstance?>()
      if (unresolvedParams.isNotEmpty()) {
         log().debug("Querying to find params for operation : $unresolvedParams")
         val paramsToSearchFor = unresolvedParams.map { QuerySpecTypeNode(it.type) }.toSet()
         val queryResult: QueryResult = context.queryEngine.find(paramsToSearchFor, context)
         if (!queryResult.isFullyResolved) {
            throw UnresolvedOperationParametersException("The following parameters could not be fully resolved : ${queryResult.unmatchedNodes}")
         }
         resolvedParams = queryResult.results
      }

      // Now, either all the params were available in the first pass,
      // or they've been subsequently resolved against the context / graph.
      // So, create a final list of values.
      val parametersWithValues = parameterValuesOrQuerySpecs.map {
         when (it) {
            is TypedInstance -> it
            is QuerySpecTypeNode -> resolvedParams[it]!!
            else -> error("Unexpected type in parameterValuesOrQuerySpecs -> ${it.javaClass.name}")
         }
      }

      return parametersWithValues
   }

   /**
    * Checks each of th parameter values against any contracts
    * specified on the inbound parameter spec.
    * If the contract is not satisfied, we attempt to satisfy the contract leveraging
    * the graph, and fail if the resolution was unsuccessful
    */
   private fun ensureParametersSatisfyContracts(parametersSpecs: List<Parameter>, parameterValues: List<TypedInstance>, context: QueryContext): List<TypedInstance> {
      val paramsWithSpec = parametersSpecs.zip(parameterValues)
      val paramsToConstraintEvaluations = paramsWithSpec.map { (paramSpec, paramValue) ->
         paramSpec to ConstraintEvaluations(paramValue, paramSpec.constraints.map { constraint -> constraint.evaluate(paramSpec, paramValue) })
      }.toMap()

      val resolvedParameterValues = constraintViolationResolver.resolveViolations(paramsToConstraintEvaluations, context, this)
      return resolvedParameterValues.values.toList()
   }

}


class UnresolvedOperationParametersException(message: String) : RuntimeException(message)
