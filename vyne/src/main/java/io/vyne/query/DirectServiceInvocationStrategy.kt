package io.vyne.query

import io.vyne.models.DefinedInSchema
import io.vyne.models.TypedCollection
import io.vyne.models.TypedInstance
import io.vyne.query.graph.operationInvocation.OperationInvocationService
import io.vyne.schemas.*
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
      return if(context.debugProfiling) {
         context.startChild(this, "look for candidate services", OperationType.LOOKUP) { profilerOperation ->
            lookForCandidateServices(context, target)
         }
      } else {
         lookForCandidateServices(context, target)
      }
   }

   private fun lookForCandidateServices(context: QueryContext, target: Set<QuerySpecTypeNode>): QueryStrategyResult {
      // TODO try caching candidate operations on the context
      val matchedNodes = getCandidateOperations(context.schema, target)
         .filter { (_, operationToParameters) -> operationToParameters.isNotEmpty() }
         .map { (queryNode, operationToParameters) ->
            val operationsToInvoke = when {
               operationToParameters.size > 1 && queryNode.mode != QueryMode.GATHER -> {
                  log().warn("Running in query mode ${queryNode.mode} and multiple candidate operations detected - ${operationToParameters.keys.joinToString { it.name }} - this isn't supported yet, will just pick the first one")
                  listOf(operationToParameters.keys.first())
               }
               queryNode.mode == QueryMode.GATHER -> operationToParameters.keys.toList()
               else -> listOf(operationToParameters.keys.first())
            }


            val serviceResults = operationsToInvoke.map { operation ->
               val parameters = operationToParameters.getValue(operation)
               val (service, _) = context.schema.operation(operation.qualifiedName)
               val serviceResult = invocationService.invokeOperation(
                  service,
                  operation,
                  context = context,
                  preferredParams = emptySet(),
                  providedParamValues = parameters.toList()
               )
               serviceResult
            }.flattenNestedTypedCollections(flattenedType = queryNode.type)

            val strategyResult = when {
               serviceResults.isEmpty() -> null
               serviceResults is TypedCollection -> serviceResults
               serviceResults.size == 1 -> serviceResults.first()
               else -> TypedCollection(queryNode.type, serviceResults) // Not sure this is a valid
            }
            queryNode to strategyResult
         }.toMap()

      return QueryStrategyResult(matchedNodes)
   }

   /**
    * Returns the operations that we can invoke, grouped by target query node.
    */
   internal fun getCandidateOperations(schema: Schema, target: Set<QuerySpecTypeNode>, requireAllParametersResolved: Boolean = true): Map<QuerySpecTypeNode, Map<Operation, Map<Parameter, TypedInstance>>> {
      val grouped =  target.map { it to getCandidateOperations(schema, it, requireAllParametersResolved) }
         .groupBy({ it.first }, { it.second })

val result = grouped.mapValues { (_, operationParameterMaps) ->
            operationParameterMaps.reduce { acc, map -> acc + map }
         }
      return result
   }

   /**
    * Returns the operations that we can invoke
    * (either because they have no parameters, or because all their parameters are populated by constraints)
    * and the set of parameters that we have identified values for
    */
   internal fun getCandidateOperations(schema: Schema, target: QuerySpecTypeNode, requireAllParametersResolved: Boolean): Map<Operation, Map<Parameter, TypedInstance>> {
      val operations =  schema.operations
         .filter { it.returnType.isAssignableTo(target.type) }
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
         }
      return operations.toMap()
   }

   private fun filterPropertyToParameterConstraint(operationConstraint: PropertyToParameterConstraint, requiredConstraint: PropertyToParameterConstraint): Boolean {
    return   operationConstraint.propertyIdentifier == requiredConstraint.propertyIdentifier
         && operationConstraint.operator == requiredConstraint.operator
         && operationConstraint.expectedValue is RelativeValueExpression
         && requiredConstraint.expectedValue is ConstantValueExpression

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
               .filter { operationConstraint -> filterPropertyToParameterConstraint(operationConstraint, requiredConstraint) }
               .map { operationConstraint ->
                  val path = (operationConstraint.expectedValue as RelativeValueExpression).path
                  val parameter = operation.parameter(path.path)
                     ?: error("Operation ${operation.name} does not expose a parameter called ${path.path}")
                  val value = (requiredConstraint.expectedValue as ConstantValueExpression).value

                  // TODO: Confirm that DefinedInSchema is appropriate here, but pretty sure these are constants.
                  val typedInstance = TypedInstance.from(parameter.type, value, schema, source = DefinedInSchema)
                  parameter to typedInstance
               }
         }.toMap()
      val allOperationConstraintsSatisfied = operationConstraintParameterValues.size == target.dataConstraints.size
      return allOperationConstraintsSatisfied to operationConstraintParameterValues
   }
}

private fun List<TypedInstance>.flattenNestedTypedCollections(flattenedType:Type): List<TypedInstance> {
   return if (this.all { it is TypedCollection }) {
      val values = this.flatMap { (it as TypedCollection).value }
      TypedCollection(flattenedType,values)
   } else {
      this
   }
}
