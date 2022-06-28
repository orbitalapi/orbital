package io.vyne.spring.invokers.http.batch

import io.vyne.models.TypedInstance
import io.vyne.schemas.*
import mu.KotlinLogging

/**
 * Capable of batching operations that do straight entity lookup.
 * eg:
 * operation findMovie(MovieId):Movie
 *
 * can be batched to
 *
 * operation findMovies(MovieId[]): Movie[]
 *
 */
class CollectionOfEntitiesStrategy : BatchingHttpStrategy {
   private val logger = KotlinLogging.logger {}
   override fun findBatchingCandidate(
      operation: RemoteOperation,
      schema: Schema,
      service: Service,
      preferredParams: Set<TypedInstance>,
      providedParamValues: List<Pair<Parameter, TypedInstance>>
   ): BatchingOperationCandidate? {
      // We don't support QueryOperations round these parts...
      if (operation is QueryOperation) {
         return null
      }
      val originalOperationReturnType = operation.returnType
      val idFields = originalOperationReturnType.getAttributesWithAnnotation("Id".fqn())
      if (idFields.isEmpty()) {
         return null
      }

      // Note: No real reason that this couldn't be supported, but needs a bit of thought.
      if (idFields.size > 1) {
         logger.info { "Operation ${operation.qualifiedName.longDisplayName} is not batchable as it's return type has a composite key, which is not currently supported" }
         return null
      }

      val modelIdField = idFields.values.single()
      val idTypeAsArrayType = schema.type(modelIdField.type).asArrayType()


      val operationsReturningCollection = service.operations
         .filter { it.returnType == operation.returnType.asArrayType() }
         .mapNotNull { batchingCandidate ->
            val paramAccumulator = inputsIndicateBatchingLookup(
               batchingCandidate,
               idTypeAsArrayType,
               schema
            )
            if (paramAccumulator == null) {
               null
            } else {
               batchingCandidate to paramAccumulator
            }
         }
      // Bail early
      return when {
         operationsReturningCollection.isEmpty() -> null
         operationsReturningCollection.size > 1 -> {
            logger.info {
               "Multiple ambiguous operations are batching candidates for ${operation.qualifiedName}, so not batching.  Matching candidates: ${
                  operationsReturningCollection.map { it.first }.joinToString { it.qualifiedName.longDisplayName }
               }"
            }
            null
         }
         else -> {
            val (batchingOperation, accumulator) = operationsReturningCollection.single()
            BatchingOperationCandidate(
               service,
               batchingOperation,
               accumulator,
               FindResultFromFieldOnResponseResultMatchingStrategy(
                  schema.type(modelIdField.type),
                  originalOperationReturnType
               )
            )
         }
      }
   }

   /**
    * Returns true if EITHER:
    *  - operation takes an input that is a collection of Ids.
    *  - OR operation takes an input model that that has a single param, which is a collection of ids.
    */
   private fun inputsIndicateBatchingLookup(
      batchingCandidate: RemoteOperation,
      idTypeAsArrayType: Type,
      schema: Schema
   ): ParameterAccumulatorStrategy? {
      if (batchingCandidate.parameters.size != 1) {
         return null
      }


      val singleInputType = batchingCandidate.parameters.single().type
      return when {
         singleInputType == idTypeAsArrayType -> AccumulateAsArray(batchingCandidate.parameters.single())
         singleInputType.attributes.size == 1 && singleInputType.attributes.values.single().type == idTypeAsArrayType.qualifiedName -> AccumulateAsArrayAttributeOnRequest(
            batchingCandidate.parameters.single(),
            schema
         )
         else -> null
      }
   }

}
