package io.vyne.models.functions.stdlib

import io.vyne.models.*
import io.vyne.models.functions.NamedFunctionInvoker
import io.vyne.models.functions.NullSafeInvoker
import io.vyne.schemas.Schema
import io.vyne.schemas.Type
import lang.taxi.functions.FunctionAccessor
import lang.taxi.types.PrimitiveType
import lang.taxi.types.QualifiedName

object ObjectFunctions {
   val functions:List<NamedFunctionInvoker> = listOf(
      Equals
   )
}

object Equals : NullSafeInvoker() {
   override val functionName: QualifiedName = lang.taxi.functions.stdlib.Equals.name

   override fun doInvoke(
      inputValues: List<TypedInstance>,
      schema: Schema,
      returnType: Type,
      function: FunctionAccessor,
      rawMessageBeingParsed: Any?,
      thisScopeValueSupplier: EvaluationValueSupplier
   ): TypedInstance {
      val areEqual = inputValues[0] == inputValues[1]
      return TypedValue.from(schema.type(PrimitiveType.BOOLEAN), areEqual, ConversionService.DEFAULT_CONVERTER, EvaluatedExpression(function.asTaxi(), inputValues))
   }


}
