package com.orbitalhq.query

import com.google.common.cache.CacheBuilder
import com.orbitalhq.models.Provided
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.query.graph.operationInvocation.OperationInvocationService
import com.orbitalhq.schemas.Operation
import com.orbitalhq.schemas.Parameter
import com.orbitalhq.schemas.RemoteOperation
import com.orbitalhq.schemas.Schema
import com.orbitalhq.schemas.StreamOperation
import com.orbitalhq.schemas.Type
import lang.taxi.expressions.Expression
import lang.taxi.expressions.LiteralExpression
import lang.taxi.services.OperationScope
import lang.taxi.types.ArgumentSelector
import mu.KotlinLogging

// Note:  Currently tested via tests in VyneTest, no direct tests, but that'd be good to add.
/**
 * Query strategy that will invoke services that return the requested type,
 * and do not require any parameters
 */
class DirectServiceInvocationStrategy(invocationService: OperationInvocationService) : QueryStrategy,
   BaseOperationInvocationStrategy(invocationService) {
   companion object {
      private val logger = KotlinLogging.logger {}
   }

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

   private suspend fun lookForCandidateServices(
      context: QueryContext,
      target: Set<QuerySpecTypeNode>
   ): Map<QuerySpecTypeNode, Map<RemoteOperation, Map<Parameter, TypedInstance>>> {
      // TODO try caching candidate operations on the context
      return getCandidateOperations(context.schema, target, context)
         .filter { (_, operationToParameters) ->
            operationToParameters.isNotEmpty()
         }

   }


   /**
    * Returns the operations that we can invoke, grouped by target query node.
    */
   private suspend fun getCandidateOperations(
      schema: Schema,
      target: Set<QuerySpecTypeNode>,
      context: QueryContext
   ): Map<QuerySpecTypeNode, Map<RemoteOperation, Map<Parameter, TypedInstance>>> {
      val grouped = mutableMapOf<QuerySpecTypeNode, MutableList<Map<RemoteOperation, Map<Parameter, TypedInstance>>>>()
      for (item in target) {
         val candidates = getCandidateOperations(schema, item, context)
         grouped.getOrPut(item) { mutableListOf() }.add(candidates)
      }

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
   private suspend fun getCandidateOperations(
      schema: Schema,
      target: QuerySpecTypeNode,
      context: QueryContext
   ): Map<RemoteOperation, Map<Parameter, TypedInstance>> {
      val operationsForType = operationsForTypeCache.get(target.type) {
         val operations: Set<RemoteOperation> = schema.operations + schema.streamOperations
         (operations).filter {
            it.returnType.isAssignableTo(target.type) && it.operationType == OperationScope.READ_ONLY
         }
      }
      val result = mutableMapOf<RemoteOperation, Map<Parameter, TypedInstance>>()
      for (operation in operationsForType) {
         val (satisfiesConstraints, operationParameters) = compareOperationContractToDataRequirementsAndFetchSearchParams(
            operation,
            target,
            schema,
            context
         )
         if (!satisfiesConstraints) continue

         val (op1, params1) = populateParamsFromContextValues(operation, operationParameters, context)
         val (op2, params2) = provideUnpopulatedParametersWithDefaults(op1, params1, context)

         // Check to see if there are any outstanding parameters that haven't been populated
         val unpopulatedParams = op2.parameters.filter { parameter ->
            !params2.containsKey(parameter) && !parameter.nullable
         }
         if (unpopulatedParams.isEmpty()) {
            result[op2] = params2
         }
      }
      return result
   }

   /**
    * Looks for any unpopulated parameters that have values present in the
    * query context (eg., from a given {} clause), and populates them
    */
   private fun populateParamsFromContextValues(
      operation: RemoteOperation,
      parameters: Map<Parameter, TypedInstance>,
      context: QueryContext
   ): Pair<RemoteOperation, Map<Parameter, TypedInstance>> {
      val populatedParams = operation.parameters
         .filter { param -> !param.type.isPrimitive } // Don't attempt to populate raw primitives, they're too ambiguous
         .filter { param -> context.hasFactOfType(param.type) }
         .associateWith { param ->
            context.getFact(param.type)
         }
      return operation to (parameters + populatedParams)
   }

   /**
    * Adds any parameters that are so far unpopulated, but
    * have a default expression that can be used to populate them
    */
   private suspend fun provideUnpopulatedParametersWithDefaults(
      operation: RemoteOperation,
      parameters: Map<Parameter, TypedInstance>,
      context: QueryContext
   ): Pair<RemoteOperation, Map<Parameter, TypedInstance>> {
      val defaultValues = operation.parameters
         .filter { it.defaultValue != null }
         .filter { !parameters.containsKey(it) }
         .associateWith { parameter ->
            context.evaluate(parameter.defaultValue!!)
      }
      return operation to (parameters + defaultValues)
   }

   /**
    * Checks to see if the operation satisfies the contract of the target (if either exist).
    * If a contract exists on the target which provides input params, and the operation
    * can satisfy the contract, then the parameters to search inputs are returned mapped.
    */
   private suspend fun compareOperationContractToDataRequirementsAndFetchSearchParams(
      remoteOperation: RemoteOperation,
      target: QuerySpecTypeNode,
      schema: Schema,
      context: QueryContext
   ): Pair<Boolean, Map<Parameter, TypedInstance>> {
      if (target.dataConstraints.isEmpty()) {
         return true to emptyMap()
      }
      if (remoteOperation is StreamOperation) {
         return true to emptyMap()
      }
      require(remoteOperation is Operation) { "Expected to find an Operation, but was type ${remoteOperation::class.simpleName}" }
      val targetDataConstraints = target.dataConstraints

      // Look at the constraints present on the target, and see if this
      // contract operation satisfies the constraints.
      // If it does, then it's possible that values in the requested constraint
      // provide inputs into parameters.
      // (eg: operation filmsPublishedAfter(date:PublicationDate):Film[](PublicationDate >= date)
      // when evaluted against a contract of
      // find { Film[](PublicationDate >= 2020-10-02)
      // would provide a value of `date`
      val satisfiedConstraints = targetDataConstraints.mapNotNull { requiredConstraint ->
         val constraintComparison = remoteOperation.contract.satisfies(requiredConstraint)
         if (constraintComparison.satisfiesRequestedConstraint) {
            requiredConstraint to getProvidedParameterValues(remoteOperation, constraintComparison.providedValues, context, schema)
         } else {
            null
         }
      }

      val allOperationConstraintsSatisfied = satisfiedConstraints.size == targetDataConstraints.size
      val operationConstraintParameterValues = satisfiedConstraints.map { it.second }
         .flatten()
         .toMap()
      return allOperationConstraintsSatisfied to operationConstraintParameterValues
   }

   private suspend fun getProvidedParameterValues(
      remoteOperation: Operation,
      providedValues: List<Pair<Expression, Expression>>,
      context: QueryContext,
      schema: Schema
   ): List<Pair<Parameter, TypedInstance>> {
      val result = mutableListOf<Pair<Parameter, TypedInstance>>()
      for ((paramExpression, providedValueExpression) in providedValues) {
         when (paramExpression) {
            is ArgumentSelector -> {
               val parameter = remoteOperation.parameter(paramExpression.scopeWithPath)
               if (parameter == null) {
                  logger.warn { "An expression was found to provide a value for parameter ${paramExpression.path}, but no such parameter exists on operation ${remoteOperation.name}" }
                  continue
               }
               // Short circut - if it's a literal (which is the most common case), then
               // provide the value
               val expressionResult = when (providedValueExpression) {
                  is LiteralExpression -> TypedInstance.from(
                     parameter!!.type,
                     providedValueExpression.value,
                     schema,
                     source = Provided
                  )

                  else -> context.evaluate(expression = providedValueExpression)
               }
               result.add(parameter to expressionResult)
            }

            else -> {
               logger.warn { "Not implemented: Mapping parameterExpression of type ${paramExpression::class.simpleName}" }
            }
         }
      }
      return result
   }
}

