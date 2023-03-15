package io.vyne.models.functions.stdlib

import io.vyne.models.*
import io.vyne.models.functions.FunctionResultCacheKey
import io.vyne.models.functions.NamedFunctionInvoker
import io.vyne.models.functions.NullSafeInvoker
import io.vyne.schemas.Schema
import io.vyne.schemas.Type
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
