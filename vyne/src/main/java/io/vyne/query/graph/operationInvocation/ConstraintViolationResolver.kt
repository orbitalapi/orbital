package io.vyne.query.graph.operationInvocation

import io.vyne.models.TypedInstance
import io.vyne.query.QueryContext
import io.vyne.schemas.*
import io.vyne.utils.log
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.runBlocking

/**
 * Given a constraint on a parameter that is violated,
 * attempts to leverage providers present in the type system
 * to resolve the conflicts, allowing Operation invokation to
 * proceed.
 *
 * eg:.  Consider an input param in the incorrect format -- the
 * resolver will attempt to find a service that will convert to the
 * correct format.
 */
class ConstraintViolationResolver {

   /**
    * Resolves violations by leveraging providers in the graph.
    * If resolution of a violation is not possible, throws an exception
    */
   suspend fun resolveViolations(evaluatedParameters: Flow<Pair<Parameter, ConstraintEvaluations>>, queryContext: QueryContext, operationEvaluator: OperationInvocationService): Flow<Pair<Parameter, TypedInstance>> {
      val resolvedParameters = evaluatedParameters.map { (param, evaluations) ->
         if (!evaluations.isValid) {
            val resolvedValue = resolveViolations(param, evaluations, queryContext, operationEvaluator)
            param to resolvedValue
         } else {
            param to evaluations.evaluatedValue
         }
      }
      return resolvedParameters
   }

   private suspend fun resolveViolations(param: Parameter, evaluations: ConstraintEvaluations, queryContext: QueryContext, operationEvaluator: OperationInvocationService): TypedInstance {
      if (evaluations.violationCount > 1) {
         // Need to consider how resolutions affect subsequent violations.
         // Given a resolution will change the value, further violations are meaningless
         // I guess, we should resolve, and then re-evaluate.
         // Need to be careful about infinite resolution loops
         throw NotImplementedError("Handling multiple violations isn't yet supported, but it should be.")
      }
      val services = queryContext.schema.services

      return evaluations
         .asSequence()
         .filter { !it.isValid }
         .map { failedEvaluation ->
            val invocationContext = findServiceToResolveConstraint(param, failedEvaluation, services)
               ?: throw UnresolvedOperationParametersException("Param ${param.type.fullyQualifiedName} failed an evaluation $failedEvaluation, but no resolution strategy was found", queryContext.evaluatedPath(), queryContext.profiler.root)

            log().warn("A blocking call is being made - ConstraintViolationResolver.resolveViolations")
            val operationResult = runBlocking {operationEvaluator.invokeOperation(invocationContext.service, invocationContext.operation, invocationContext.discoveredParams.toSet(), queryContext).first() }

            failedEvaluation.violation!!.resolveWithUpdatedValue(operationResult)
         }.first()
   }

   private fun findServiceToResolveConstraint(param: Parameter, evaluation: ConstraintEvaluation, services: Set<Service>): OperationInvocationContext? {
      val operationContexts = services.flatMap { service ->
         service.operations.map { operation -> service to operation }
      }
         .map { (service, operation) ->
            Triple(service, operation, evaluation.violation!!.provideResolutionAdvice(operation, operation.contract))
         }
         .filter { (_, _, resolutionAdvice) -> resolutionAdvice != null }
         .map { (service, operation, resolutionAdvice: ResolutionAdvice?) ->
            OperationInvocationContext.from(service, operation, resolutionAdvice!!)
         }
      // TODO : Consider returning multiple matching operations, and invoking them
      // until one succeeds
//         .firstOrNull()
      return operationContexts.firstOrNull()
   }

   data class OperationInvocationContext(val service: Service, val operation: Operation,
      // Note : I think this is a silly idea, returning TypedInstance list
      // as we may lose context.
      // However, the intent is that functions should leverage type aliases
      // for highly descriptive signatures, so it should be alright.
                                         val discoveredParams: List<TypedInstance>) {
      companion object {
         fun from(service: Service, operation: Operation, resolutionAdvice: ResolutionAdvice): OperationInvocationContext {
            val suggestedParams = operation.parameters.mapNotNull { param ->
               if (resolutionAdvice.containsValueForParam(param)) {
                  resolutionAdvice.getParamValue(param)
               } else {
                  null
               }
            }
            return OperationInvocationContext(service, operation, suggestedParams)
         }
      }
   }


}
