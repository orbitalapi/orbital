package com.orbitalhq.models.functions.stdlib.math

import com.orbitalhq.models.EvaluatedExpression
import com.orbitalhq.models.EvaluationValueSupplier
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.models.TypedValue
import com.orbitalhq.models.functions.FunctionResultCacheKey
import com.orbitalhq.models.functions.NullSafeInvoker
import com.orbitalhq.schemas.Schema
import com.orbitalhq.schemas.Type
import lang.taxi.functions.FunctionAccessor
import lang.taxi.types.FormatsAndZoneOffset
import lang.taxi.types.QualifiedName
import java.math.BigDecimal
import java.math.RoundingMode

object Round : NullSafeInvoker() {
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
      val inputValue = inputValues[0].toRawObject() as BigDecimal
      val precision = inputValues[1].toRawObject() as Int
      val rounded = inputValue.setScale(precision, RoundingMode.HALF_DOWN)
      return TypedInstance.from(returnType, rounded, schema, source = EvaluatedExpression(function.asTaxi(), inputValues))
   }

   override val functionName: QualifiedName = lang.taxi.functions.stdlib.Round.name
}
