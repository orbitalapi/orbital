package io.vyne.query.graph.operationInvocation

import io.vyne.models.TypedInstance
import io.vyne.query.QueryContext
import io.vyne.query.connectors.batch.OperationBatchingStrategy
import io.vyne.schemas.Parameter
import io.vyne.schemas.RemoteOperation
import io.vyne.schemas.Schema
import io.vyne.schemas.Service
import kotlinx.coroutines.flow.Flow
import mu.KotlinLogging

/**
 * When asked to invoke a service, will first check to see if
 *
 * This decorates the OperationInvocationService, rather than the
 * OperationInvoker, as it may change the service we actually invoke.
 * Also, in order to make that decision, we need access to the query context,
 * and the set of invokers.
 *
 */
class BatchingOperationInvocationServiceDecorator(
   private val operationBatchingStrategies: List<OperationBatchingStrategy>,
   private val invocationService: OperationInvocationService,
   constraintViolationResolver: ConstraintViolationResolver = ConstraintViolationResolver(),
) : AbstractOperationInvocationService(constraintViolationResolver) {

   private val logger = KotlinLogging.logger {}
   var batchingEnabled: Boolean = false
      get() {
         return field
      }
      set(value) {
         logger.info { "Batching of operations set to $value" }
         field = value;
      }


   override suspend fun invokeOperation(
      service: Service,
      operation: RemoteOperation,
      preferredParams: Set<TypedInstance>,
      context: QueryContext,
      providedParamValues: List<Pair<Parameter, TypedInstance>>
   ): Flow<TypedInstance> {
      if (!context.isProjecting) {
         return invocationService.invokeOperation(service, operation, preferredParams, context, providedParamValues)
      } else {
         val batchingStrategy =
            findBatchingStrategy(service, operation, context.schema, preferredParams, providedParamValues)
         return if (batchingStrategy == null) {
            logger.debug { "Operation ${operation.qualifiedName.longDisplayName} is not batchable" }
            invocationService.invokeOperation(service, operation, preferredParams, context, providedParamValues)
         } else {
            batchedInvocation(service, operation, preferredParams, context, providedParamValues, batchingStrategy)
         }

      }
   }

   private suspend fun batchedInvocation(
      service: Service,
      operation: RemoteOperation,
      preferredParams: Set<TypedInstance>,
      context: QueryContext,
      providedParamValues: List<Pair<Parameter, TypedInstance>>,
      batchingStrategy: OperationBatchingStrategy
   ): Flow<TypedInstance> {
      logger.debug { "Batching invocation request to ${operation.qualifiedName}" }
      val parameters = gatherParameters(operation.parameters, preferredParams, context, providedParamValues)
      return batchingStrategy.invokeInBatch(
         service,
         operation,
         preferredParams,
         parameters,
         context,
         context.schema,
         null
      )
   }

   private fun findBatchingStrategy(
      service: Service,
      operation: RemoteOperation,
      schema: Schema,
      preferredParams: Set<TypedInstance>,
      providedParamValues: List<Pair<Parameter, TypedInstance>>
   ): OperationBatchingStrategy? {
      return operationBatchingStrategies.firstOrNull {
         it.canBatch(
            service,
            operation,
            schema,
            preferredParams,
            providedParamValues
         )
      }
   }

}
