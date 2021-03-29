package io.vyne.query.graph.operationInvocation

import io.vyne.models.TypedInstance
import io.vyne.models.TypedNull
import io.vyne.query.ProfilerOperation
import io.vyne.query.QueryContext
import io.vyne.query.QueryResult
import io.vyne.query.QuerySpecTypeNode
import io.vyne.query.SearchFailedException
import io.vyne.query.graph.EdgeEvaluator
import io.vyne.query.graph.EvaluatableEdge
import io.vyne.query.graph.EvaluatedEdge
import io.vyne.query.graph.EvaluatedLink
import io.vyne.query.graph.LinkEvaluator
import io.vyne.query.graph.ParameterFactory
import io.vyne.schemas.ConstraintEvaluations
import io.vyne.schemas.Link
import io.vyne.schemas.Parameter
import io.vyne.schemas.QualifiedName
import io.vyne.schemas.Relationship
import io.vyne.schemas.RemoteOperation
import io.vyne.schemas.Service
import io.vyne.schemas.fqn
import io.vyne.utils.log
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.runBlocking
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component

/**
 * The parent to OperationInvokers
 */
interface OperationInvocationService {
   suspend fun invokeOperation(service: Service, operation: RemoteOperation, preferredParams: Set<TypedInstance>, context: QueryContext, providedParamValues: List<Pair<Parameter, TypedInstance>> = emptyList()): Flow<TypedInstance>
}

interface OperationInvoker {
   fun canSupport(service: Service, operation: RemoteOperation): Boolean

   fun invoke(service: Service, operation: RemoteOperation, parameters: List<Pair<Parameter, TypedInstance>>, profilerOperation: ProfilerOperation): Flow<TypedInstance>
}

class DefaultOperationInvocationService(private val invokers: List<OperationInvoker>, private val constraintViolationResolver: ConstraintViolationResolver = ConstraintViolationResolver()) : OperationInvocationService {
   override suspend fun invokeOperation(
      service: Service,
      operation: RemoteOperation,
      preferredParams: Set<TypedInstance>,
      context: QueryContext,
      providedParamValues: List<Pair<Parameter, TypedInstance>>
   ): Flow<TypedInstance> {
      val invoker = invokers.firstOrNull { it.canSupport(service, operation) }
         ?: throw IllegalArgumentException("No invokers found for Operation ${operation.name}")

      val parameters = gatherParameters(operation.parameters, preferredParams, context, providedParamValues)
      val resolvedParams = ensureParametersSatisfyContracts(parameters, context)
      return invoker.invoke(service, operation, resolvedParams.toList(), context)
   }

   private suspend fun gatherParameters(parameters: List<Parameter>, candidateParamValues: Set<TypedInstance>, context: QueryContext, providedParamValues: List<Pair<Parameter, TypedInstance>>): Flow<Pair<Parameter, TypedInstance>> {
      // NOTE : See DirectServiceInvocationStrategy, where we have an alternative approach for gatehring params.
      // Suggest merging that here.

      val preferredParamsByType = candidateParamValues.associateBy { it.type }
      val unresolvedParams = mutableListOf<QuerySpecTypeNode>()
      // Holds EITHER the param value, or a QuerySpecTypeNode which can be used
      // to query the engine for a value.
      val parameterValuesOrQuerySpecs: List<Pair<Parameter, Any>> = parameters
         // Filter out the params that we've already been provided
         .filter { requiredParam -> providedParamValues.none { (providedParam, _) -> requiredParam == providedParam } }
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
      var resolvedParams : Flow<TypedInstance>? = flow {}
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

      val parametersWithValues:Flow<Pair<Parameter, TypedInstance>> = parameterValuesOrQuerySpecs.map { (param, valueOrQuerySpec) ->
         when (valueOrQuerySpec) {
            is TypedInstance -> param to valueOrQuerySpec
            is QuerySpecTypeNode -> param to resolvedParams?.firstOrNull()!!
            else -> error("Unexpected type in parameterValuesOrQuerySpecs -> ${valueOrQuerySpec.javaClass.name}")

         }
      }.asFlow()

      return flowOf(parametersWithValues, providedParamValues.asFlow()).flattenMerge()

   }

   /**
    * Checks each of th parameter values against any contracts
    * specified on the inbound parameter spec.
    * If the contract is not satisfied, we attempt to satisfy the contract leveraging
    * the graph, and fail if the resolution was unsuccessful
    */
   private suspend fun ensureParametersSatisfyContracts(parametersWithValues: Flow<Pair<Parameter, TypedInstance>>, context: QueryContext): Flow<Pair<Parameter, TypedInstance>> {


      val paramsToConstraintEvaluations = parametersWithValues.map { (paramSpec, paramValue) ->
         paramSpec to ConstraintEvaluations(paramValue,
            paramSpec.constraints.map { constraint ->
               constraint.evaluate(paramSpec.type, paramValue, context.schema)
            }
         )
      }

      val resolvedParameterValues = constraintViolationResolver.resolveViolations(paramsToConstraintEvaluations, context, this)
      return resolvedParameterValues
   }
}

@Component
class OperationInvocationEvaluator(val invocationService: OperationInvocationService, val parameterFactory: ParameterFactory = ParameterFactory()) : LinkEvaluator, EdgeEvaluator {
   override suspend fun evaluate(edge: EvaluatableEdge, context: QueryContext): EvaluatedEdge {


      val operationName: QualifiedName = (edge.vertex1.value as String).fqn()
      val (service, operation) = context.schema.operation(operationName)

      // Discover parameters.

      val parameterValues = operation.parameters.map { requiredParam ->

         try {
            // Note: We can't always assume that the inbound relationship has taken care of this
            // for us, as we don't know what path was travelled to arrive here.
            if (edge.previousValue != null && edge.previousValue.type.isAssignableTo(requiredParam.type)) {
               edge.previousValue
            } else {
               val paramInstance = parameterFactory.discover(requiredParam.type, context, operation)
               context.addFact(paramInstance)
               paramInstance
            }


         } catch (e: Exception) {
            log().warn("Failed to discover param of type ${requiredParam.type.fullyQualifiedName} for operation ${operation.qualifiedName} - ${e::class.simpleName} ${e.message}")
            return edge.failure(null)
         }
      }

      val callArgs = parameterValues.toSet()
      if (context.hasOperationResult(edge, callArgs as Set<TypedInstance>)) {
         val cachedResult = context.getOperationResult(edge, callArgs)
         cachedResult?.let { context.addFact(it) }
         return  edge.success(cachedResult)
      }

      return try {
         val result: TypedInstance = invocationService.invokeOperation(service, operation, callArgs, context).first()
         if (result is TypedNull) {
            log().info("Operation ${operation.qualifiedName} returned null with a successful response.  Will treat this as a success, but won't store the result")
         } else {
            context.addFact(result)
         }
         context.addOperationResult(edge, result, callArgs)
         edge.success(result)

      } catch (exception: Exception) {
         // Operation invokers throw exceptions for failed invocations.
         // Don't throw here, just report the failure
         log().warn("Operation ${operation.qualifiedName} failed with exception ${exception.message}.  This is often ok, as services throwing exceptions is expected.")
         edge.failure(null, failureReason = "Operation ${operation.qualifiedName} failed with exception ${exception.message}")
      }

   }

   override val relationship: Relationship = Relationship.PROVIDES

   override suspend fun evaluate(link: Link, startingPoint: TypedInstance, context: QueryContext): EvaluatedLink {
      TODO("I'm not sure if this is still used")
      val operationName = link.start
      val (service, operation) = context.schema.operation(operationName)

      val result: Flow<TypedInstance> = invocationService.invokeOperation(service, operation, setOf(startingPoint), context)

      var linkResult: TypedInstance
      runBlocking {
         result.collect { r -> context.addFact(r) }
         linkResult = result.first()
      }
      return EvaluatedLink(link, startingPoint, linkResult)
   }


}

class SearchRuntimeException(exception: Exception, operation: ProfilerOperation) : SearchFailedException("The search failed with an exception: ${exception.message}", listOf(), operation)

class UnresolvedOperationParametersException(message: String, evaluatedPath: List<EvaluatedEdge>, operation: ProfilerOperation) : SearchFailedException(message, evaluatedPath, operation)

class OperationInvocationException(message: String, val httpStatus:HttpStatus) : RuntimeException(message)
