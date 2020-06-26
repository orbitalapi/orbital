package io.vyne.query.graph.operationInvocation

import io.vyne.models.TypedInstance
import io.vyne.query.*
import io.vyne.query.graph.*
import io.vyne.schemas.*
import io.vyne.utils.log
import org.springframework.stereotype.Component

/**
 * The parent to OperationInvokers
 */
interface OperationInvocationService {
   fun invokeOperation(service: Service, operation: Operation, preferredParams: Set<TypedInstance>, context: QueryContext, providedParamValues: List<Pair<Parameter, TypedInstance>> = emptyList()): TypedInstance
}

interface OperationInvoker {
   fun canSupport(service: Service, operation: Operation): Boolean
   // TODO : This should return some form of reactive type.
   fun invoke(service: Service, operation: Operation, parameters: List<Pair<Parameter, TypedInstance>>, profilerOperation: ProfilerOperation): TypedInstance
}

class DefaultOperationInvocationService(private val invokers: List<OperationInvoker>, private val constraintViolationResolver: ConstraintViolationResolver = ConstraintViolationResolver()) : OperationInvocationService {
   override fun invokeOperation(service: Service, operation: Operation, preferredParams: Set<TypedInstance>, context: QueryContext, providedParamValues: List<Pair<Parameter, TypedInstance>>): TypedInstance {
      val invoker = invokers.firstOrNull { it.canSupport(service, operation) }
         ?: throw IllegalArgumentException("No invokers found for Operation ${operation.name}")

      val parameters = gatherParameters(operation.parameters, preferredParams, context, providedParamValues)
      val resolvedParams = ensureParametersSatisfyContracts(parameters, context)
      val result: TypedInstance = invoker.invoke(service, operation, resolvedParams, context)

      return result
   }

   private fun gatherParameters(parameters: List<Parameter>, candidateParamValues: Set<TypedInstance>, context: QueryContext, providedParamValues:List<Pair<Parameter,TypedInstance>>): List<Pair<Parameter, TypedInstance>> {
      // NOTE : See DirectServiceInvocationStrategy, where we have an alternative approach for gatehring params.
      // Suggest merging that here.

      val preferredParamsByType = candidateParamValues.associateBy { it.type }
      val unresolvedParams = mutableListOf<QuerySpecTypeNode>()
      // Holds EITHER the param value, or a QuerySpecTypeNode which can be used
      // to query the engine for a value.
      val parameterValuesOrQuerySpecs: List<Pair<Parameter, Any>> = parameters
         // Filter out the params that we've already been provided
         .filter { requiredParam -> providedParamValues.none { (providedParam,_) -> requiredParam == providedParam } }
         .map { requiredParam ->
         val preferredParam = candidateParamValues.firstOrNull { it.type.resolvesSameAs(requiredParam.type) }
         when {
            preferredParam != null -> requiredParam to preferredParam
            preferredParamsByType.containsKey(requiredParam.type) -> requiredParam to preferredParamsByType.getValue(requiredParam.type)
            context.hasFactOfType(requiredParam.type) -> requiredParam to context.getFact(requiredParam.type)
            else -> {
               val queryNode = QuerySpecTypeNode(requiredParam.type)
               unresolvedParams.add(queryNode)
               requiredParam to queryNode
            }
         }
      }

      // Try to resolve any unresolved params
      var resolvedParams = emptyMap<QuerySpecTypeNode, TypedInstance?>()
      if (unresolvedParams.isNotEmpty()) {
         log().debug("Querying to find params for Operation : ${unresolvedParams.map { it.type.fullyQualifiedName }}")
         val paramsToSearchFor = unresolvedParams.map { QuerySpecTypeNode(it.type) }.toSet()
         val queryResult: QueryResult = context.queryEngine.find(paramsToSearchFor, context)
         if (!queryResult.isFullyResolved) {
            throw UnresolvedOperationParametersException("The following parameters could not be fully resolved : ${queryResult.unmatchedNodes}", context.evaluatedPath(), context.profiler.root)
         }
         resolvedParams = queryResult.results
      }

      // Now, either all the params were available in the first pass,
      // or they've been subsequently resolved against the context / graph.
      // So, create a final list of values.
      val parametersWithValues = parameterValuesOrQuerySpecs.map { (param, valueOrQuerySpec) ->
         when (valueOrQuerySpec) {
            is TypedInstance -> param to valueOrQuerySpec
            is QuerySpecTypeNode -> param to resolvedParams[valueOrQuerySpec]!!
            else -> error("Unexpected type in parameterValuesOrQuerySpecs -> ${valueOrQuerySpec.javaClass.name}")
         }
      } + providedParamValues

      return parametersWithValues
   }

   /**
    * Checks each of th parameter values against any contracts
    * specified on the inbound parameter spec.
    * If the contract is not satisfied, we attempt to satisfy the contract leveraging
    * the graph, and fail if the resolution was unsuccessful
    */
   private fun ensureParametersSatisfyContracts(parametersWithValues: List<Pair<Parameter, TypedInstance>>, context: QueryContext): List<Pair<Parameter, TypedInstance>> {
      val paramsToConstraintEvaluations = parametersWithValues.map { (paramSpec, paramValue) ->
         paramSpec to ConstraintEvaluations(paramValue,
            paramSpec.constraints.map { constraint ->
               constraint.evaluate(paramSpec.type, paramValue, context.schema)
            }
         )
      }.toMap()

      val resolvedParameterValues = constraintViolationResolver.resolveViolations(paramsToConstraintEvaluations, context, this)
      return resolvedParameterValues.toList()
   }
}

@Component
class OperationInvocationEvaluator(val invocationService: OperationInvocationService, val parameterFactory: ParameterFactory = ParameterFactory()) : LinkEvaluator, EdgeEvaluator {
   override fun evaluate(edge: EvaluatableEdge, context: QueryContext): EvaluatedEdge {
      if (context.hasOperationResult(edge)) {
         val cachedResult = context.getOperationResult(edge)
         return edge.success(cachedResult)
      }

      val operationName: QualifiedName = (edge.vertex1.value as String).fqn()
      val (service, operation) = context.schema.operation(operationName)

      // Discover parameters.
      // Note: We can't always assume that the REQUIRES_PARAM relationship has taken care of this
      // for us, as we don't know what path was travelled to arrive here.
      // Therefore, just find all the params, and add them to the context.
      // This will fail if a param is not discoverable
      operation.parameters.forEach { requiredParam ->
         val paramInstance = parameterFactory.discover(requiredParam.type, context)
         context.addFact(paramInstance)
      }

      // VisitedNodes are better candidates for params, as they are more contextually relevant
      val visitedInstanceNodes = context.collectVisitedInstanceNodes()


      val result: TypedInstance = invocationService.invokeOperation(service, operation, visitedInstanceNodes, context)
      // Consider enhancing facts with the information how we derived them, e.g. via Operation so that can be used as cache?
      context.addFact(result)
      context.addOperationResult(edge, result)
      return edge.success(result)
   }

   override val relationship: Relationship = Relationship.PROVIDES

   override fun evaluate(link: Link, startingPoint: TypedInstance, context: QueryContext): EvaluatedLink {
      TODO("I'm not sure if this is still used")
      val operationName = link.start
      val (service, operation) = context.schema.operation(operationName)
      val result: TypedInstance = invocationService.invokeOperation(service, operation, setOf(startingPoint), context)
      context.addFact(result)
      return EvaluatedLink(link, startingPoint, result)
   }


}

class SearchRuntimeException(exception: Exception, operation: ProfilerOperation) : SearchFailedException("The search failed with an exception: ${exception.message}", listOf(), operation)

class UnresolvedOperationParametersException(message: String, evaluatedPath: List<EvaluatedEdge>, operation: ProfilerOperation) : SearchFailedException(message, evaluatedPath, operation)

class OperationInvocationException(message: String) : RuntimeException(message)
