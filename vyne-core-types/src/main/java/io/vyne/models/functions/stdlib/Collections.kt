package io.vyne.models.functions.stdlib

import io.vyne.models.EvaluatedExpression
import io.vyne.models.TypedCollection
import io.vyne.models.TypedInstance
import io.vyne.models.functions.NamedFunctionInvoker
import io.vyne.models.functions.NullSafeInvoker
import io.vyne.schemas.Schema
import io.vyne.schemas.Type
import lang.taxi.functions.FunctionAccessor
import lang.taxi.types.QualifiedName

object Collections {
   val functions: List<NamedFunctionInvoker> = listOf(
      Contains
   )
}

object Contains : NullSafeInvoker() {
   override val functionName: QualifiedName = lang.taxi.functions.stdlib.Contains.name
   override fun doInvoke(
      inputValues: List<TypedInstance>,
      schema: Schema,
      returnType: Type,
      function: FunctionAccessor
   ): TypedInstance {
      val collection = inputValues[0] as TypedCollection
      val searchTarget = inputValues[1] as TypedInstance
      val result = collection.any { it.valueEquals(searchTarget) }
      val dataSource = EvaluatedExpression(function.asTaxi(), inputValues)
      return TypedInstance.from(returnType, result, schema, source = dataSource)
   }

}
