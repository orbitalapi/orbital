package com.orbitalhq.query.graph.operationInvocation

import com.orbitalhq.models.*
fimport com.orbitalhq.query.*
import com.orbitalhq.query.connectors.OperationInvoker
import com.orbitalhq.query.graph.edges.EdgeEvaluator
import com.orbitalhq.query.graph.edges.EvaluatableEdge
import com.orbitalhq.query.graph.edges.EvaluatedEdge
import com.orbitalhq.query.graph.edges.ParameterFactory
import com.orbitalhq.schemas.*
import com.orbitalhq.utils.StrategyPerformanceProfiler
import com.orbitalhq.utils.log
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.toList
import mu.KotlinLogging
import java.time.Duration
import java.time.Instant

private val logger = KotlinLogging.logger {}

/**
 * The parent to OperationInvokers
 */
interface OperationInvocationService {
   suspend fun invokeOperation(
      service: Service,
      operation: RemoteOperation,
      preferredParams: Set<TypedInstance>,
      context: QueryContext,
      providedParamValues: List<Pair<Parameter, TypedInstance>> = emptyList()
   ): Flow<TypedInstance>
}

class DefaultOperationInvocationService(
   private val invokers: List<OperationInvoker>,
   private val constraintViolationResolver: ConstraintViolationResolver = ConstraintViolationResolver()
) : OperationInvocationService {
   override suspend fun invokeOperation(
      service: Service,
      operation: RemoteOperation,
      preferredParams: Set<TypedInstance>,
      context: QueryContext,
      providedParamValues: List<Pair<Parameter, TypedInstance>>
   ): Flow<TypedInstance> {
      val invoker = invokers.firstOrNull { it.canSupport(service, operation) }
         ?: throw IllegalArgumentException("No invokers found for Operation ${operation.name}")

      val paramStart = Instant.now()
      val parameters = try {
         gatherParameters(operation.parameters, preferredParams, context, providedParamValues, operation)
      } catch (e:Exception) {
         log().error("Gather params failed", e)
         throw e
      }
      val resolvedParams = ensureParametersSatisfyContracts(parameters, context)
      val validatedParams = resolvedParams
      StrategyPerformanceProfiler.record(
         "OperationInvocation.gatherParameters",
         Duration.between(paramStart, Instant.now())
      )

      val startTime = Instant.now()
      val result = invoker.invoke(service, operation, validatedParams, context, context.queryId)
      StrategyPerformanceProfiler.record(
         "OperationInvocationService.invoker.invoke",
         Duration.between(startTime, Instant.now())
      )
//       context.addOperationResult(edge, result, callArgs)
      return result

//         .onEach { logger.info { "Operation invoker saw result" } }
   }

   private suspend fun gatherParameters(
      parameters: List<Parameter>,
      candidateParamValues: Set<TypedInstance>,
      context: QueryContext,
      providedParamValues: List<Pair<Parameter, TypedInstance>>,
      operation: RemoteOperation
   ): List<Pair<Parameter, TypedInstance>> {
      if (parameters.isEmpty()) {
         return emptyList()
      }
      // NOTE : See DirectServiceInvocationStrategy, where we have an alternative approach for gatehring params.
      // Suggest merging that here.
      val startTime = Instant.now()
      val preferredParamsByType = candidateParamValues.associateBy { it.type }
      val unresolvedParams = mutableListOf<Pair<Parameter,QuerySpecTypeNode>>()
      // Holds EITHER the param value, or a QuerySpecTypeNode which can be used
      // to query the engine for a value.
      val parameterValuesOrQuerySpecs: List<Pair<Parameter, Any>> = parameters
         // Filter out the params that we've already been provided
         .filter { requiredParam -> providedParamValues.none { (providedParam, _) -> requiredParam == providedParam } }
         .map { requiredParam ->
            val preferredParam = candidateParamValues.firstOrNull { it.type.resolvesSameAs(requiredParam.type) }
            when {
               preferredParam != null -> requiredParam to preferredParam
               preferredParamsByType.containsKey(requiredParam.type) -> requiredParam to preferredParamsByType.getValue(
                  requiredParam.type
               )
               context.hasFactOfType(requiredParam.type) -> requiredParam to context.getFact(requiredParam.type)
               requiredParam.type.isPrimitive -> {
                  logger.warn { "Operation ${operation.qualifiedName} has a parameter ${requiredParam.name} with type ${requiredParam.type} - constructing requests with primtiive types is not supported - use a semantic type instead" }
                  requiredParam to TypedNull.create(requiredParam.type)
               }
               else -> {
                  val queryNode = QuerySpecTypeNode(requiredParam.type)
                  unresolvedParams.add(requiredParam to queryNode)
                  requiredParam to queryNode
               }
            }
         }

      // Try to resolve any unresolved params
      val resolvedParams = unresolvedParams.map { (param,paramQuerySpec) ->
         val failureBehaviour = if (param.nullable) FailureBehaviour.THROW else FailureBehaviour.SEND_TYPED_NULL
         val queryResult = context.queryEngine.find(paramQuerySpec, context, failureBehaviour = failureBehaviour)
         when {
             !queryResult.isFullyResolved && !param.nullable -> {
                 throw UnresolvedOperationParametersException(
                    "The following parameters could not be fully resolved : ${queryResult.unmatchedNodes}",
                    context.evaluatedPath(),
                    context.profiler.root,
                    // TODO : Surface the failed attempts
                    emptyList()
                 )
             }
             else -> paramQuerySpec to queryResult.results.toList().first()
         }
      }.toMap()
//      val resolvedParams: List<TypedInstance> = if (unresolvedParams.isNotEmpty()) {
//         logger.debug { "Querying to find params for Operation : ${unresolvedParams.map { it.type.fullyQualifiedName }}" }
//         val paramsToSearchFor = unresolvedParams.map { QuerySpecTypeNode(it.type) }.toSet()
//         val queryResult: QueryResult = context.queryEngine.find(paramsToSearchFor, context)
//         if (!queryResult.isFullyResolved) {
//            throw UnresolvedOperationParametersException(
//               "The following parameters could not be fully resolved : ${queryResult.unmatchedNodes}",
//               context.evaluatedPath(),
//               context.profiler.root,
//               // TODO : Surface the failed attempts
//               emptyList()
//            )
//         }
//         queryResult.results.toList()
//      } else {
//         emptyList()
//      }

      // Now, either all the params were available in the first pass,
      // or they've been subsequently resolved against the context / graph.
      // So, create a final list of values.
      val parametersWithValues: List<Pair<Parameter, TypedInstance>> =
         parameterValuesOrQuerySpecs.map { (param, valueOrQuerySpec) ->
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
   private suspend fun ensureParametersSatisfyContracts(
      parametersWithValues: List<Pair<Parameter, TypedInstance>>,
      context: QueryContext
   ): List<Pair<Parameter, TypedInstance>> {


      val paramsToConstraintEvaluations = parametersWithValues.map { (paramSpec, paramValue) ->
         paramSpec to ConstraintEvaluations(paramValue,
            paramSpec.constraints.map { constraint ->
               constraint.evaluate(paramSpec.type, paramValue, context.schema)
            }
         )
      }

      val resolvedParameterValues =
         constraintViolationResolver.resolveViolations(paramsToConstraintEvaluations, context, this)
            .toList()
      return resolvedParameterValues
   }
}


val numberOfCores = Runtime.getRuntime().availableProcessors()

class OperationInvocationEvaluator(
   val invocationService: OperationInvocationService,
   val parameterFactory: ParameterFactory = ParameterFactory()
) : EdgeEvaluator {
   override suspend fun evaluate(edge: EvaluatableEdge, context: QueryContext): EvaluatedEdge {

      val operationName: QualifiedName = (edge.vertex1.value as String).fqn()
      val (service, operation) = context.schema.remoteOperation(operationName)

      // Discover parameters.
      val parameterValues = collectParameters(operation, edge, context)

      val callArgs = parameterValues.toSet()
      if (context.hasOperationResult(operationName.fullyQualifiedName, callArgs as Set<TypedInstance>)) {
         val cachedResult = context.getOperationResult(operationName.fullyQualifiedName, callArgs)
         cachedResult?.let {
            //ADD RESULT TO CONTEXT
            //context.addFact(it)
         }
         edge.success(cachedResult)
      }

      try {

         val result = invokeOperation(service, operation, callArgs, context)

         if (result is TypedNull) {
            logger.info { "Operation ${operation.qualifiedName} (called with args $callArgs) returned a successful response of null.  Will treat this as a success, but won't add the result to the search context" }
         } else {
            //ADD RESULT TO CONTEXT
            //context.addFact(result)
         }
         context.notifyOperationResult(edge, result, callArgs)
         return edge.success(result)
      } catch (exception: Exception) {
         val dataSource = when (exception) {
            is OperationInvocationException -> OperationResult.from(exception.parameters, exception.remoteCall)
               .asOperationReferenceDataSource()

            else -> FailedEvaluation("An error occurred when invoking operation ${operation.qualifiedName.longDisplayName}: ${exception.message} ")
         }
         // Operation invokers throw exceptions for failed invocations.
         // Don't throw here, just report the failure
         val result = TypedNull.create(type = operation.returnType, source = dataSource)
         context.notifyOperationResult(edge, result, callArgs)
         logger.warn { "Operation ${operation.qualifiedName} (called with $callArgs) failed with exception ${exception.message}. " }
         return edge.failure(
            result,
            failureReason = "Operation ${operation.qualifiedName} ktor exception ${exception.message}"
         )
      }

   }

   private suspend fun invokeOperation(
      service: Service,
      operation: RemoteOperation,
      callArgs: Set<TypedInstance>,
      context: QueryContext
   ): TypedInstance {
      // Danger - this .toList() call won't work with streaming queries!
      // Needs to be fixed in 0.19
      return invocationService.invokeOperation(service, operation, callArgs, context).toList()
         .let { typedInstances ->
            when {
               operation.returnType.isCollection -> TypedCollection.arrayOf(
                  operation.returnType.collectionType!!,
                  typedInstances,
                  MixedSources.singleSourceOrMixedSources(typedInstances)
               )
               typedInstances.isEmpty() -> {
                  // This is a weak fallback.  Ideally, upstream should've provided a TypedNull with a FailedSearch,
                  // as they have reference to the RemoteCall, but we don't.
                  TypedNull.create(
                     operation.returnType,
                     source = FailedSearch("Call to ${operation.qualifiedName} with args $callArgs returned no results")
                  )
               }
               typedInstances.size == 1 -> {
                  typedInstances.first()
               }
               else -> {
                  logger.error { "Operation ${operation.qualifiedName} is not expected to return a collection, but yielded a collection of size ${typedInstances.size}.  The first value is being taken, and the rest ignored." }
                  typedInstances.first()
               }
            }
         }
   }

   private suspend fun collectParameters(
      operation: RemoteOperation,
      edge: EvaluatableEdge,
      context: QueryContext
   ):List<TypedInstance> = operation.parameters.mapNotNull { requiredParam ->

      try {
         // Note: We can't always assume that the inbound relationship has taken care of this
         // for us, as we don't know what path was travelled to arrive here.
         when {
            edge.previousValue != null && edge.previousValue.type.isAssignableTo(requiredParam.type) -> edge.previousValue
            else -> parameterFactory.discover(requiredParam.type, context, edge.previousValue, operation)
         }
      } catch (e: Exception) {
         logger.warn { "Failed to discover param of type ${requiredParam.type.fullyQualifiedName} for operation ${operation.qualifiedName} - ${e::class.simpleName} ${e.message}" }
         null
      }
   }

   override val relationship: Relationship = Relationship.PROVIDES
}

class SearchRuntimeException(exception: Exception, operation: ProfilerOperation) :
   SearchFailedException("The search failed with an exception: ${exception.message}", listOf(), operation, emptyList())

class UnresolvedOperationParametersException(
   message: String,
   evaluatedPath: List<EvaluatedEdge>,
   operation: ProfilerOperation,
   failedAttempts: List<DataSource>
) : SearchFailedException(message, evaluatedPath, operation, failedAttempts)


