package io.vyne.spring.invokers.http.batch

import io.vyne.models.TypedCollection
import io.vyne.models.TypedInstance
import io.vyne.models.TypedObject
import io.vyne.schemas.Type
import lang.taxi.types.AttributePath

/**
 * When a result is returned from a batched operation,
 * the ResultMatchingStrategy is responsible for looking at a result entry, and
 * finding the corresponding originating call.
 */
interface ResultMatchingStrategy {

   /**
    * Optional pre-send hook, which allows the strategy
    * to look at the batch going out, and prepare any indexes etc.,
    * to allow efficient processing of responses when they arrive.
    *
    * No-op is totally fine.
    */
   fun beforeSend(batch: List<BatchedOperation>) {}


   /**
    * Takes a result returned from a service call, and matches it to the
    * originating BatchedOperation request.
    *
    * Different result structures may mean that a single result matches
    * multiple triggering BatchedOperations
    */
   fun findOriginatingOperation(
      batch: List<BatchedOperation>,
      typedObject: TypedObject
   ): List<Pair<BatchedOperation, TypedInstance>>
}

/**
 * On a given response, looks for the value of a field.
 * That field is expected to be present on the outbound operation, as well as on the
 * result recieved back.
 *
 */
class FindResultFromFieldOnResponseResultMatchingStrategy(
   private val fieldType: Type,
   private val originalOperationReturnType: Type
) : ResultMatchingStrategy {
   //   private lateinit var originatingOperationById: Map<TypedInstance, BatchedOperation>
   override fun beforeSend(batch: List<BatchedOperation>) {

//      if (this::originatingOperationById.isInitialized) {
//         error("This result matching strategy has already been prepared.  It is expected they're single use.")
//      }
//      originatingOperationById = groupByIdField(batch, fieldType)
   }

   override fun findOriginatingOperation(
      batch: List<BatchedOperation>,
      typedObject: TypedObject
   ): List<Pair<BatchedOperation, TypedInstance>> {
      val originatingOperationById = groupByIdField(batch, fieldType)
      val idValue = typedObject.getAttributeIdentifiedByType(fieldType)
      val originatingRequest = originatingOperationById[idValue]
         ?: error("Could not find originating request for ${typedObject.type.longDisplayName} with id ${idValue.value?.toString()}")
      val originalResultValue = typedObject.getAttributeIdentifiedByType(originalOperationReturnType)
      return listOf(
         originatingRequest to originalResultValue
      )
   }

   private fun groupByIdField(batch: List<BatchedOperation>, idFieldType: Type): Map<TypedInstance, BatchedOperation> {
      return batch.associateBy { operation ->
         val idParamPair = operation.parameters.single { it.first.type == idFieldType }
         idParamPair.second
      }
   }
}

class FindMultipleResultsFromFieldOnResponse(
   private val indexType: Type,
   private val attributePath: AttributePath,
   originalOperationReturnType: Type
) : ResultMatchingStrategy {
   // Interanlly, we defer all the actual resolution to FindResultFromFieldOnResponseResultMatchingStrategy,
   // once we've unwrapped the result to the correct values.
   private val internalStrategy = FindResultFromFieldOnResponseResultMatchingStrategy(
      indexType,
      originalOperationReturnType
   )

   override fun beforeSend(batch: List<BatchedOperation>) {
      internalStrategy.beforeSend(batch)
   }

   override fun findOriginatingOperation(
      batch: List<BatchedOperation>,
      typedObject: TypedObject
   ): List<Pair<BatchedOperation, TypedInstance>> {
      val collectionAtPath = typedObject.get(attributePath)
      require(collectionAtPath is TypedCollection)

      // Unwrap the collection members on the result object, matching them
      // to the correct request object.
      val innerResults = collectionAtPath.map { member ->
         val internalMatchedOperation = internalStrategy.findOriginatingOperation(batch, member as TypedObject)
         require(internalMatchedOperation.size == 1) { "Expected to match a single result for result $member but found ${internalMatchedOperation.size}" }
         internalMatchedOperation.single()
      }
      return innerResults
   }

}
