package io.vyne.spring.invokers.http.batch

import io.vyne.models.TypedInstance
import io.vyne.schemas.*
import lang.taxi.types.AttributePath
import mu.KotlinLogging

/**
 *
 * Can batch something where an operation exists that takes a collection of the input parameters,
 * and returns a collection of the response objects, indexed by an @Id attribute.
 *
 *
 * eg:
 * model Actor { .. }
 * model Film {
 *   @Id filmId : FilmId
 * }
 *
 * operation findActors(FilmId):Actor[]
 *
 * can be batched to
 *
 * model ActorLookupRequest {
 *    filmIds:FilmId[] // A collection of ids
 * }
 * model ActorLookupResult {
 *    filmId : FilmId // the Id
 *    actors: Actor[] // The result of the original request.
 * }
 * operation findActors(ActorLookupRequest): ActorLookupResult[]
 */
class BatchRequestIsMappableCollectionOfSingleRequestResponseObjectsStrategy : BatchingHttpStrategy {
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

      // Must take a single input
      if (operation.parameters.size != 1) {
         return null
      }
      val singleParam = operation.parameters.single()

      val returnType = operation.returnType

      val candidateOperations = service.operations
         .mapNotNull { candidateOperation ->
            // Find the appropriate batching strategy, if any exist.
            // If none exist, returns null, which will trigger an exit.
            // Otherwise, returns the parameterAccumulatorStrategy to use
            sequenceOf(
               {
                  // operation foo(T):? can potentially batched by operation foo(T[]):?
                  operationTakesArrayOfParam(candidateOperation, singleParam.type)
               },
               {
                  // operation foo(T):? can potentially batched by operation foo({ x:T }[]):?
                  operationTakesModelContainingArrayOfParam(candidateOperation, singleParam.type, schema)
               },
            ).mapNotNull { it() }
               .firstOrNull()
               ?.let { strategy -> candidateOperation to strategy }
         }
         .mapNotNull { (candidateOperation, parameterAccumulatorStrategy) ->
            // Find the appropriate result matching strategy, if any exist.
            // If none exist, returns null, which will trigger an exit.
            operationReturnsACollectionOfResponseIndexedByAnInputField(
               candidateOperation,
               singleParam.type,
               returnType,
               schema
            )?.let { resultMatchingStrategy ->
               BatchingOperationCandidate(
                  service,
                  candidateOperation,
                  parameterAccumulatorStrategy,
                  resultMatchingStrategy
               )
            }
         }

      if (candidateOperations.isEmpty()) {
         return null
      }
      if (candidateOperations.size > 1) {
         logger.debug { "Can't batch operation ${operation.qualifiedName.longDisplayName} as there were multiple candidates - ${candidateOperations.joinToString()}" }
         return null
      }

      return candidateOperations.single()
   }

   private fun operationReturnsACollectionOfResponseIndexedByAnInputField(
      candidateOperation: Operation,
      requiredIndexType: Type,
      originalOperationReturnType: Type,
      schema: Schema
   ): ResultMatchingStrategy? {

      val candidateOperationReturnType = candidateOperation.returnType
      return returnTypeIsACollectionOfResponseIndexedByInputField(
         candidateOperationReturnType,
         originalOperationReturnType,
         requiredIndexType,
         schema
      )
   }

   private fun returnTypeIsACollectionOfResponseIndexedByInputField(
      candidateOperationReturnType: Type,
      originalOperationReturnType: Type,
      requiredIndexType: Type,
      schema: Schema,
      attributePath: AttributePath = AttributePath.EMPTY
   ): ResultMatchingStrategy? {
      if (candidateOperationReturnType.isCollection) {
         return returnTypeIsACollectionOfResponseIndexedByInputField(
            candidateOperationReturnType.collectionType!!,
            originalOperationReturnType,
            requiredIndexType,
            schema
         )
      }

      if (candidateOperationReturnType.isScalar) {
         return null
      }

      val responseContainsOriginalReturnType = candidateOperationReturnType.attributes.values.any {
         it.type == originalOperationReturnType.qualifiedName
      }
      val responseIsIndexedByOriginalInputType = candidateOperationReturnType.attributes.values.any {
         it.type == requiredIndexType.qualifiedName
      }

      if (responseIsIndexedByOriginalInputType && responseContainsOriginalReturnType) {
         // We can match these.
         return FindResultFromFieldOnResponseResultMatchingStrategy(requiredIndexType, originalOperationReturnType)
      }

      // If there's a property present on the result object, we can use
      // that, but we need a different strategy, since one property will yield
      // mulitple results.
      return candidateOperationReturnType.attributes
         .asSequence()
         .mapNotNull { (attributeName, field) ->
            val fieldType = schema.type(field.type)
            val thisFieldAttributePath = attributePath.append(attributeName)
            returnTypeIsACollectionOfResponseIndexedByInputField(
               fieldType,
               originalOperationReturnType,
               requiredIndexType,
               schema,
               thisFieldAttributePath
            )?.let { foundStrategy ->
               FindMultipleResultsFromFieldOnResponse(
                  requiredIndexType,
                  thisFieldAttributePath,
                  originalOperationReturnType
               )
            }
         }
         .firstOrNull()
   }

   private fun operationTakesModelContainingArrayOfParam(
      candidateOperation: Operation,
      operationParamType: Type,
      schema: Schema
   ): ParameterAccumulatorStrategy? {
      if (candidateOperation.parameters.size != 1) {
         return null
      }
      if (candidateOperation.parameters.single().type.attributes.size != 1) {
         return null
      }
      val singleInputParam = candidateOperation.parameters.single().type.attributes.values.single().type
      return if (schema.type(singleInputParam) == operationParamType.asArrayType()) {
         AccumulateAsArrayAttributeOnRequest(
            candidateOperation.parameters.single(), schema
         )
      } else {
         null
      }
   }

   private fun operationTakesArrayOfParam(
      candidateOperation: Operation,
      operationParamType: Type
   ): ParameterAccumulatorStrategy? {
      return if (candidateOperation.parameters.size == 1
         && candidateOperation.parameters.single().type == operationParamType.asArrayType()
      ) {
         AccumulateAsArray(
            candidateOperation.parameters.single()
         )
      } else {
         null
      }
   }


}
