package io.vyne.query.graph.operationInvocation

import io.vyne.models.DataSource
import io.vyne.models.FailedEvaluation
import io.vyne.models.FailedSearch
import io.vyne.models.OperationResult
import io.vyne.models.TypedCollection
import io.vyne.models.TypedInstance
import io.vyne.models.TypedNull
import io.vyne.query.ProfilerOperation
import io.vyne.query.QueryContext
import io.vyne.query.QueryContextEventDispatcher
import io.vyne.query.QuerySpecTypeNode
import io.vyne.query.RemoteCall
import io.vyne.query.SearchFailedException
import io.vyne.query.graph.EdgeEvaluator
import io.vyne.query.graph.EvaluatableEdge
import io.vyne.query.graph.EvaluatedEdge
import io.vyne.query.graph.EvaluatedLink
import io.vyne.query.graph.LinkEvaluator
import io.vyne.query.graph.ParameterFactory
import io.vyne.schemas.ConstraintEvaluations
import io.vyne.schemas.Link
import io.vyne.schemas.Operation
import io.vyne.schemas.Parameter
import io.vyne.schemas.QualifiedName
import io.vyne.schemas.Relationship
import io.vyne.schemas.RemoteOperation
import io.vyne.schemas.Service
import io.vyne.schemas.fqn
import io.vyne.utils.StrategyPerformanceProfiler
import kotlinx.coroutines.ExecutorCoroutineDispatcher
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.toList
import mu.KotlinLogging
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import java.time.Duration
import java.time.Instant
import java.util.concurrent.Executors

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

interface OperationInvoker {
   fun canSupport(service: Service, operation: RemoteOperation): Boolean

   suspend fun invoke(
      service: Service,
      operation: RemoteOperation,
      parameters: List<Pair<Parameter, TypedInstance>>,
      eventDispatcher: QueryContextEventDispatcher,
      queryId: String? = null
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
      val parameters = gatherParameters(operation.parameters, preferredParams, context, providedParamValues)
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
      return result
//         .onEach { logger.info { "Operation invoker saw result" } }
   }

   private suspend fun gatherParameters(
      parameters: List<Parameter>,
      candidateParamValues: Set<TypedInstance>,
      context: QueryContext,
      providedParamValues: List<Pair<Parameter, TypedInstance>>
   ): List<Pair<Parameter, TypedInstance>> {
      if (parameters.isEmpty()) {
         return emptyList()
      }
      // NOTE : See DirectServiceInvocationStrategy, where we have an alternative approach for gatehring params.
      // Suggest merging that here.
      val startTime = Instant.now()
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
               preferredParamsByType.containsKey(requiredParam.type) -> requiredParam to preferredParamsByType.getValue(
                  requiredParam.type
               )
               context.hasFactOfType(requiredParam.type) -> requiredParam to context.getFact(requiredParam.type)
               else -> {
                  val queryNode = QuerySpecTypeNode(requiredParam.type)
                  unresolvedParams.add(queryNode)
                  requiredParam to queryNode
               }
            }
         }

      // Try to resolve any unresolved params
      val resolvedParams = unresolvedParams.map {
         val queryResult = context.queryEngine.find(it, context)
         if (!queryResult.isFullyResolved) {
            throw UnresolvedOperationParametersException(
               "The following parameters could not be fully resolved : ${queryResult.unmatchedNodes}",
               context.evaluatedPath(),
               context.profiler.root,
               // TODO : Surface the failed attempts
               emptyList()
            )
         }
         it to queryResult.results.toList().first()
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

val dispatcher = Executors.newFixedThreadPool(32).asCoroutineDispatcher()

val numberOfCores = Runtime.getRuntime().availableProcessors()
val operationInvocationEvaluatorispatcher: ExecutorCoroutineDispatcher =
   Executors.newFixedThreadPool(32).asCoroutineDispatcher()

@Component
class OperationInvocationEvaluator(
   val invocationService: OperationInvocationService,
   val parameterFactory: ParameterFactory = ParameterFactory()
) : LinkEvaluator, EdgeEvaluator {
   override suspend fun evaluate(edge: EvaluatableEdge, context: QueryContext): EvaluatedEdge {

      val operationName: QualifiedName = (edge.vertex1.value as String).fqn()
      val (service, operation) = context.schema.operation(operationName)

      // Discover parameters.
      val parameterValues = collectParameters(operation, edge, context)

      val callArgs = parameterValues.toSet()
      if (context.hasOperationResult(edge, callArgs as Set<TypedInstance>)) {
         val cachedResult = context.getOperationResult(edge, callArgs)
         cachedResult?.let {
         //   context.addFact(it)
         }
         edge.success(cachedResult)
      }

      try {

         val result = invokeOperation(service, operation, callArgs, context)

         if (result is TypedNull) {
            logger.debug { "Operation ${operation.qualifiedName} (called with args $callArgs) returned a successful response of null.  Will treat this as a success, but won't add the result to the search context" }
         } else {
            //context.addFact(result)
         }
         context.addOperationResult(edge, result, callArgs)
         return edge.success(result)
      } catch (exception: Exception) {
         val dataSource = when (exception) {
            is OperationInvocationException -> OperationResult.from(exception.parameters, exception.remoteCall)
            else -> FailedEvaluation("An error occurred when invoking operation ${operation.qualifiedName.longDisplayName}: ${exception.message} ")
         }
         // Operation invokers throw exceptions for failed invocations.
         // Don't throw here, just report the failure
         val result = TypedNull.create(type = operation.returnType, source = dataSource)
         context.addOperationResult(edge, result, callArgs)
         logger.debug { "Operation ${operation.qualifiedName} (called with $callArgs) failed with exception ${exception.message}.  This is often ok, as services throwing exceptions is expected." }
         return edge.failure(
            result,
            failureReason = "Operation ${operation.qualifiedName} failed with exception ${exception.message}"
         )
      }

   }

   private suspend fun invokeOperation(
      service: Service,
      operation: Operation,
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
                  typedInstances
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
      operation: Operation,
      edge: EvaluatableEdge,
      context: QueryContext
   ) = operation.parameters.map { requiredParam ->

      try {
         // Note: We can't always assume that the inbound relationship has taken care of this
         // for us, as we don't know what path was travelled to arrive here.
         if (edge.previousValue != null && edge.previousValue.type.isAssignableTo(requiredParam.type)) {
            edge.previousValue
         } else {
            val paramInstance = parameterFactory.discover(requiredParam.type, context, operation)
            //context.addFact(paramInstance)
            paramInstance
         }


      } catch (e: Exception) {
         logger.warn { "Failed to discover param of type ${requiredParam.type.fullyQualifiedName} for operation ${operation.qualifiedName} - ${e::class.simpleName} ${e.message}" }
         edge.failure(null)
      }
   }

   override val relationship: Relationship = Relationship.PROVIDES

   override suspend fun evaluate(link: Link, startingPoint: TypedInstance, context: QueryContext): EvaluatedLink {
      TODO("I'm not sure if this is still used")
   }
}

class SearchRuntimeException(exception: Exception, operation: ProfilerOperation) :
   SearchFailedException("The search failed with an exception: ${exception.message}", listOf(), operation, emptyList())

class UnresolvedOperationParametersException(
   message: String,
   evaluatedPath: List<EvaluatedEdge>,
   operation: ProfilerOperation,
   failedAttempts: List<DataSource>
) : SearchFailedException(message, evaluatedPath, operation, failedAttempts)

class OperationInvocationException(
   message: String,
   val httpStatus: HttpStatus,
   val remoteCall: RemoteCall,
   val parameters: List<Pair<Parameter, TypedInstance>>
) : RuntimeException(message)
