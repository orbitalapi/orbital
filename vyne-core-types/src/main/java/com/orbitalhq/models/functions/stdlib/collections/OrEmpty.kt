package com.orbitalhq.models.functions.stdlib.collections

import com.orbitalhq.models.EvaluatedExpression
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

object OrEmpty : NamedFunctionInvoker { // Intentionally not NullSafeInvoker() as the first argument is often null
   override val functionName: QualifiedName = lang.taxi.functions.stdlib.OrEmpty.name

   override suspend fun invoke(
      inputValues: List<TypedInstance>,
      schema: Schema,
      returnType: Type,
      function: FunctionAccessor,
      objectFactory: EvaluationValueSupplier,
      returnTypeFormat: FormatsAndZoneOffset?,
      rawMessageBeingParsed: Any?,
      resultCache: MutableMap<FunctionResultCacheKey, Any>
   ): TypedInstance {
      val input = inputValues[0]
      return if (input is TypedNull) {
         if (input.type.isCollection) {
            TypedCollection.empty(input.type, source = EvaluatedExpression(function.asTaxi(), inputValues))
         } else {
            createFailureWithTypedNull("Expected to receive a collection, or a null collection - but got a null of type ${input.type.name.shortDisplayName}", returnType, function, inputValues)
         }
      } else {
         input
      }
   }
}
