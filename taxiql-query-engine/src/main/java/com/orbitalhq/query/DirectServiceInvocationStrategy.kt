package com.orbitalhq.query

import com.google.common.cache.CacheBuilder
import com.orbitalhq.models.DefinedInSchema
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.query.graph.operationInvocation.OperationInvocationService
import com.orbitalhq.schemas.*
import com.orbitalhq.utils.log
import lang.taxi.services.operations.constraints.ConstantValueExpression
import lang.taxi.services.operations.constraints.RelativeValueExpression

// Note:  Currently tested via tests in VyneTest, no direct tests, but that'd be good to add.
/**
 * Query strategy that will invoke services that return the requested type,
 * and do not require any parameters
 */
class DirectServiceInvocationStrategy(invocationService: OperationInvocationService) : QueryStrategy,
   BaseOperationInvocationStrategy(invocationService) {
   private val operationsForTypeCache = CacheBuilder.newBuilder()
      .weakKeys()
      .build<Type, List<RemoteOperation>>()

   override suspend fun invoke(
      target: Set<QuerySpecTypeNode>,
      context: QueryContext,
      invocationConstraints: InvocationConstraints
   ): QueryStrategyResult {


      /**

      Commenting out this to fulfill the following scneario:

      model Item {
      id: Id
      }

      service SecurityService {
      operation allSecurities(): Item[]
      operation currentStock(): NumberOfItems
      }

      find { Item[] }  as {
      isin: Isin
      currentStock: NumberOfItems
      }[]

      in the above query 'currentStock' can only be populated by currentStock() operation through this strategy during projection.
      Below check put in place to avoid infinite recursions. However, it doesn't seem to be valid anymore.

      if (context.isProjecting) {
      return QueryStrategyResult.searchFailed()
      }
       */

      val operations = lookForCandidateServices(context, target)
      return invokeOperations(operations, context, target)
   }

   private fun lookForCandidateServices(
      context: QueryContext,
      target: Set<QuerySpecTypeNode>
   ): Map<QuerySpecTypeNode, Map<RemoteOperation, Map<Parameter, TypedInstance>>> {
      // TODO try caching candidate operations on the context
      return getCandidateOperations(context.schema, target)
         .filter { (_, operationToParameters) -> operationToParameters.isNotEmpty() }

   }


   /**
    * Returns the operations that we can invoke, grouped by target query node.
    */
   internal fun getCandidateOperations(
      schema: Schema,
      target: Set<QuerySpecTypeNode>,
      requireAllParametersResolved: Boolean = true
   ): Map<QuerySpecTypeNode, Map<RemoteOperation, Map<Parameter, TypedInstance>>> {
      val grouped = target.map { it to getCandidateOperations(schema, it, requireAllParametersResolved) }
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
   internal fun getCandidateOperations(
      schema: Schema,
      target: QuerySpecTypeNode,
      requireAllParametersResolved: Boolean
   ): Map<RemoteOperation, Map<Parameter, TypedInstance>> {
      var operationsForType = operationsForTypeCache.get(target.type) {
         val operations: Set<RemoteOperation> = schema.operations + schema.streamOperations
         (operations).filter {
            it.returnType.isAssignableTo(target.type)
         }
      }
      val operations = operationsForType
         .mapNotNull { operation ->
            val (satisfiesConstraints, operationParameters) = compareOperationContractToDataRequirementsAndFetchSearchParams(
               operation,
               target,
               schema
            )
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

   private fun filterPropertyToParameterConstraint(
      operationConstraint: PropertyToParameterConstraint,
      requiredConstraint: PropertyToParameterConstraint
   ): Boolean {
      return operationConstraint.propertyIdentifier == requiredConstraint.propertyIdentifier
         && operationConstraint.operator == requiredConstraint.operator
         && operationConstraint.expectedValue is RelativeValueExpression
         && requiredConstraint.expectedValue is ConstantValueExpression

   }

   /**
    * Checks to see if the operation satisfies the contract of the target (if either exist).
    * If a contract exists on the target which provides input params, and the operation
    * can satisfy the contract, then the parameters to search inputs are returned mapped.
    */
   private fun compareOperationContractToDataRequirementsAndFetchSearchParams(
      remoteOperation: RemoteOperation,
      target: QuerySpecTypeNode,
      schema: Schema
   ): Pair<Boolean, Map<Parameter, TypedInstance>> {
      if (target.dataConstraints.isEmpty()) {
         return true to emptyMap()
      }
      if (remoteOperation is StreamOperation) {
         return true to emptyMap()
      }
      require(remoteOperation is Operation) { "Expected to find an Operation, but was type ${remoteOperation::class.simpleName}" }
      val unevaluatableConstraints = target.dataConstraints.filter { it !is PropertyToParameterConstraint }
      if (unevaluatableConstraints.isNotEmpty()) {
         log().warn("Operation ${remoteOperation.name} has constraints that we haven't built support for.  Will not be evaluated")
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
            remoteOperation.contract.constraints
               .filterIsInstance<PropertyToParameterConstraint>()
               .filter { operationConstraint ->
                  filterPropertyToParameterConstraint(
                     operationConstraint,
                     requiredConstraint
                  )
               }
               .map { operationConstraint ->
                  val path = (operationConstraint.expectedValue as RelativeValueExpression).path
                  val parameter = remoteOperation.parameter(path.path)
                     ?: error("Operation ${remoteOperation.name} does not expose a parameter called ${path.path}")
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
