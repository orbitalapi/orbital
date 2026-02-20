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

object IfEmpty : NullSafeInvoker() {
   override val functionName: QualifiedName = lang.taxi.functions.stdlib.IfEmpty.name


   override suspend fun doInvoke(
      inputValues: List<TypedInstance>,
      schema: Schema,
      returnType: Type,
      function: FunctionAccessor,
      rawMessageBeingParsed: Any?,
      thisScopeValueSupplier: EvaluationValueSupplier,
      returnTypeFormat: FormatsAndZoneOffset?,
      resultCache: MutableMap<FunctionResultCacheKey, Any>
   ): TypedInstance {
      val sourceCollection = inputValues[0]
      if (sourceCollection !is TypedCollection) {
         return createFailureWithTypedNull("Expected arg 0 to be a collection, but found ${sourceCollection::class.simpleName} of ${sourceCollection.typeName}", returnType, function, inputValues)
      }
      val defaultValue = inputValues[1]
      if (defaultValue !is TypedCollection) {
         return createFailureWithTypedNull("Expected arg 1 to be a collection, but found ${defaultValue::class.simpleName} of ${defaultValue.typeName}", returnType, function, inputValues)
      }
      return if (sourceCollection.isEmpty()) {
         defaultValue
      } else {
         sourceCollection
      }
   }


}
