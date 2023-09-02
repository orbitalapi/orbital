package com.orbitalhq.models.functions.stdlib.collections

import com.orbitalhq.models.EvaluationValueSupplier
import com.orbitalhq.models.TypedCollection
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.models.TypedNull
import com.orbitalhq.models.functions.FunctionResultCacheKey
import com.orbitalhq.models.functions.NamedFunctionInvoker
import com.orbitalhq.schemas.Schema
import com.orbitalhq.schemas.Type
import lang.taxi.functions.FunctionAccessor
import lang.taxi.types.FormatsAndZoneOffset
import lang.taxi.types.QualifiedName


abstract class CollectionNavigatingFunction : NamedFunctionInvoker, CollectionFilteringFunction() {

   override fun invoke(
      inputValues: List<TypedInstance>,
      schema: Schema,
      returnType: Type,
      function: FunctionAccessor,
      objectFactory: EvaluationValueSupplier,
      returnTypeFormat: FormatsAndZoneOffset?,
      rawMessageBeingParsed: Any?,
      resultCache: MutableMap<FunctionResultCacheKey, Any>
   ): TypedInstance {

      val collection = when (val collection = inputValues[0]) {
         is TypedNull -> {
            return createFailureWithTypedNull(
               "Expected a collection in param 0, but got null.",
               returnType, function, inputValues
            )
         }

         is TypedCollection -> collection
         else -> return createFailureWithTypedNull(
            "Expected either a TypedNull or a TypedCollection in param 0.  Got a ${collection::class.simpleName}",
            returnType, function, inputValues
         )
      }

      return select(collection, inputValues) { message ->
         createFailureWithTypedNull(message, returnType, function, inputValues)
      }
   }

   abstract fun select(
      collection: List<TypedInstance>,
      inputValues: List<TypedInstance>,
      failureFactory: (String) -> TypedNull
   ): TypedInstance
}

object First : CollectionNavigatingFunction() {
   override fun select(
      collection: List<TypedInstance>,
      inputValues: List<TypedInstance>,
      failureFactory: (String) -> TypedNull
   ): TypedInstance {
      return collection.firstOrNull() ?: failureFactory("The provided collection was empty")
   }

   override val functionName: QualifiedName = lang.taxi.functions.stdlib.First.name
}

object Last : CollectionNavigatingFunction() {
   override fun select(
      collection: List<TypedInstance>,
      inputValues: List<TypedInstance>,
      failureFactory: (String) -> TypedNull
   ): TypedInstance {
      return collection.lastOrNull() ?: failureFactory("The provided collection was empty")
   }

   override val functionName: QualifiedName = lang.taxi.functions.stdlib.Last.name
}

object GetAtIndex : CollectionNavigatingFunction() {
   override fun select(
      collection: List<TypedInstance>,
      inputValues: List<TypedInstance>,
      failureFactory: (String) -> TypedNull
   ): TypedInstance {
      if (collection.isEmpty()) {
         return failureFactory("The provided collection was empty")
      }
      val index = inputValues.getOrNull(1)
         ?: return failureFactory("${functionName.typeName} expects an index in parameter index 1, but nothing was provided")
      val indexValue = when (val indexValue = index.value) {
         null -> return failureFactory("The provided index was null")
         is Number -> indexValue.toInt()
         is String -> indexValue.toIntOrNull()
            ?: return failureFactory("The provided index value ('$indexValue') is not a valid number")

         else -> return failureFactory("Could not work out how to parse a number from the provided value of '$indexValue'")
      }

      return collection.getOrNull(indexValue)
         ?: failureFactory("Index $indexValue is out of bounds of the provided collection, which has ${collection.size} elements")
   }

   override val functionName: QualifiedName = lang.taxi.functions.stdlib.GetAtIndex.name
}
