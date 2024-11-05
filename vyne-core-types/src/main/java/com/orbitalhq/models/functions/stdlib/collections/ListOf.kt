package com.orbitalhq.models.functions.stdlib.collections

import com.orbitalhq.models.EvaluatedExpression
import com.orbitalhq.models.EvaluationValueSupplier
import com.orbitalhq.models.TypedCollection
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.models.functions.FunctionResultCacheKey
import com.orbitalhq.models.functions.NamedFunctionInvoker
import com.orbitalhq.schemas.Schema
import com.orbitalhq.schemas.Type
import lang.taxi.functions.FunctionAccessor
import lang.taxi.functions.stdlib.ListOf
import lang.taxi.types.FormatsAndZoneOffset
import lang.taxi.types.QualifiedName

object ListOf : NamedFunctionInvoker {
   override val functionName: QualifiedName = lang.taxi.functions.stdlib.ListOf.name

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
      val dataSource = EvaluatedExpression(function.asTaxi(), inputValues)
      val collection = if (inputValues.isEmpty()) {
         TypedCollection.empty(returnType)
      } else {
         TypedCollection.from(inputValues, dataSource)
      }
      return collection
   }
}
