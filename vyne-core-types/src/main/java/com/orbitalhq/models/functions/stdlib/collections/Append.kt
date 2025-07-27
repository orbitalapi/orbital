package com.orbitalhq.models.functions.stdlib.collections

import com.orbitalhq.models.EvaluatedExpression
import com.orbitalhq.models.EvaluationValueSupplier
import com.orbitalhq.models.TypedCollection
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.models.functions.FunctionResultCacheKey
import com.orbitalhq.models.functions.NullSafeInvoker
import com.orbitalhq.schemas.Schema
import com.orbitalhq.schemas.Type
import lang.taxi.functions.FunctionAccessor
import lang.taxi.types.FormatsAndZoneOffset
import lang.taxi.types.QualifiedName

object Append : NullSafeInvoker() {
   override fun doInvoke(
      inputValues: List<TypedInstance>,
      schema: Schema,
      returnType: Type,
      function: FunctionAccessor,
      rawMessageBeingParsed: Any?,
      thisScopeValueSupplier: EvaluationValueSupplier,
      returnTypeFormat: FormatsAndZoneOffset?,
      resultCache: MutableMap<FunctionResultCacheKey, Any>
   ): TypedInstance {
      val array1 = inputValues[0]
      if (array1 !is TypedCollection) {
         return createFailureWithTypedNull("Expected arg 0 to be a TypedCollection, but was ${array1::class.simpleName}", returnType, function, inputValues)
      }
      val array2 = inputValues[1]
      if (array2 !is TypedCollection) {
         return createFailureWithTypedNull("Expected arg 1 to be a TypedCollection, but was ${array2::class.simpleName}", returnType, function, inputValues)
      }

      val allItems = array1 + array2
      return TypedCollection(returnType, allItems, EvaluatedExpression(function.asTaxi(), inputValues))
   }

   override val functionName: QualifiedName = lang.taxi.functions.stdlib.Append.name
}
