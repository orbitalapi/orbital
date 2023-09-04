package com.orbitalhq.models.functions.stdlib

import com.orbitalhq.models.*
import com.orbitalhq.models.functions.FunctionResultCacheKey
import com.orbitalhq.models.functions.NamedFunctionInvoker
import com.orbitalhq.models.functions.NullSafeInvoker
import com.orbitalhq.schemas.Schema
import com.orbitalhq.schemas.Type
import lang.taxi.functions.FunctionAccessor
import lang.taxi.types.FormatsAndZoneOffset
import lang.taxi.types.PrimitiveType
import lang.taxi.types.QualifiedName

object ObjectFunctions {
   val functions:List<NamedFunctionInvoker> = listOf(
      Equals
   )
}

object Equals : NamedFunctionInvoker {
   override val functionName: QualifiedName = lang.taxi.functions.stdlib.Equals.name

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
      val areEqual = inputValues[0] == inputValues[1]
      return TypedValue.from(schema.type(PrimitiveType.BOOLEAN), areEqual, ConversionService.DEFAULT_CONVERTER, EvaluatedExpression(function.asTaxi(), inputValues))
   }
}
