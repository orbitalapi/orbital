package io.vyne.query

import io.vyne.models.TypedInstance
import io.vyne.query.graph.operationInvocation.OperationInvocationService
import io.vyne.schemas.Operation
import io.vyne.schemas.Parameter
import io.vyne.schemas.PropertyToParameterConstraint
import io.vyne.schemas.Schema
import io.vyne.utils.log
import lang.taxi.services.operations.constraints.ConstantValueExpression
import lang.taxi.services.operations.constraints.RelativeValueExpression
import lang.taxi.types.AttributePath
import org.springframework.stereotype.Component

// Note:  Currently tested via tests in VyneTest, no direct tests, but that'd be good to add.
/**
 * Query strategy that will invoke services that return the requested type,
 * and do not require any parameters
 */
@Component
class DirectServiceInvocationStrategy(private val invocationService: OperationInvocationService) : QueryStrategy {
   override fun invoke(target: Set<QuerySpecTypeNode>, context: QueryContext): QueryStrategyResult {

      return context.startChild(this, "look for candidate services", OperationType.LOOKUP) { profilerOperation ->
         val matchedNodes = getCandidateOperations(context.schema, target)
            .filter { (_,operationToParameters) -> operationToParameters.isNotEmpty() }
            .map { (queryNode, operationToParameters) ->
               if (operationToParameters.size > 1) {
                  log().warn("Multiple candidate operations detected - ${operationToParameters.keys.joinToString { it.name }} - this isn't supported yet, will just pick the first one")
               }
               val operation = operationToParameters.keys.first()
               val parameters = operationToParameters.getValue(operation)
               val (service, _) = context.schema.operation(operation.qualifiedName)
               val serviceResult = invocationService.invokeOperation(service, operation, emptySet(), context, parameters.toList())
               queryNode to serviceResult
            }.toMap()

         QueryStrategyResult(matchedNodes)
      }

   }

   /**
    * Returns the operations that we can invoke, grouped by target query node.
    */
   internal fun getCandidateOperations(schema: Schema, target: Set<QuerySpecTypeNode>, requireAllParametersResolved: Boolean = true): Map<QuerySpecTypeNode, Map<Operation, Map<Parameter, TypedInstance>>> {
      return target.map { it to getCandidateOperations(schema, it, requireAllParametersResolved) }
         .groupBy({ it.first }, { it.second })
         .mapValues { (querySpecTypeNode, operationParameterMaps) ->
            operationParameterMaps.reduce { acc, map -> acc + map }
         }
   }

   /**
    * Returns the operations that we can invoke
    * (either because they have no parameters, or because all their parameters are populated by constraints)
    * and the set of parameters that we have identified values for
    */
   internal fun getCandidateOperations(schema: Schema, target: QuerySpecTypeNode, requireAllParametersResolved: Boolean): Map<Operation, Map<Parameter, TypedInstance>> {
      return schema.operations
         .filter { it.returnType.resolvesSameAs(target.type) }
         .mapNotNull { operation ->
            val (satisfiesConstraints, operationParameters) = compareOperationContractToDataRequirementsAndFetchSearchParams(operation, target, schema)
            if (!satisfiesConstraints) {
               null
            } else {
               operation to operationParameters
            }
         }
         .filter { (operation, populatedOperationParameters) ->
            if (requireAllParametersResolved) {
               // Check to see if there are any outstanding parameters that haven't been populated
               val unpopulatedParams = operation.parameters.filter { parameter ->
                  !populatedOperationParameters.containsKey(parameter)
               }
               unpopulatedParams.isEmpty()
            } else {
               true
            }
         }.toMap()
   }

   /**
    * Checks to see if the operation satisfies the contract of the target (if either exist).
    * If a contract exists on the target which provides input params, and the operation
    * can satisfy the contract, then the parameters to search inputs are returned mapped.
    */
   private fun compareOperationContractToDataRequirementsAndFetchSearchParams(operation: Operation, target: QuerySpecTypeNode, schema: Schema): Pair<Boolean, Map<Parameter, TypedInstance>> {
      if (target.dataConstraints.isEmpty()) {
         return true to emptyMap()
      }
      val unevaluatableConstraints = target.dataConstraints.filter { it !is PropertyToParameterConstraint }
      if (unevaluatableConstraints.isNotEmpty()) {
         log().warn("Operation ${operation.name} has constraints that we haven't built support for.  Will not be evaluated")
         return false to emptyMap()
      }
      // This approach is a first pass, and far from ideal.  It's far too concrete and tightly coupled
      // to survive the long-term.
      // Look to see if the service has declared a contract, then
      // look to see if the contract satisfies our target's requirements contract.
      // (This only works with PropertyToParameterConstraint types at the moment, which is
      // moderately general-purpose).
      // Then capture the values from the constraint passed and return them to use
      // in the invocation of the operation.

      val operationConstraintParameterValues: Map<Parameter, TypedInstance> = target.dataConstraints
         .filterIsInstance<PropertyToParameterConstraint>() // everything by this stage
         .flatMap { requiredConstraint ->
            operation.contract.constraints
               .filterIsInstance<PropertyToParameterConstraint>()
               .filter { operationConstraint ->
                  operationConstraint.propertyIdentifier == requiredConstraint.propertyIdentifier
                     && operationConstraint.operator == requiredConstraint.operator
                     && operationConstraint.expectedValue is RelativeValueExpression
                     && requiredConstraint.expectedValue is ConstantValueExpression
               }
               .map { operationConstraint ->
                  val path = (operationConstraint.expectedValue as RelativeValueExpression).path
                  val parameter = operation.parameter(path.path)
                     ?: error("Operation ${operation.name} does not expose a parameter called ${path.path}")
                  val value = (requiredConstraint.expectedValue as ConstantValueExpression).value

                  val typedInstance = TypedInstance.from(parameter.type, value, schema)
                  parameter to typedInstance
               }
         }.toMap()
      val allOperationConstraintsSatisfied = operationConstraintParameterValues.size == target.dataConstraints.size
      return allOperationConstraintsSatisfied to operationConstraintParameterValues
   }
}
