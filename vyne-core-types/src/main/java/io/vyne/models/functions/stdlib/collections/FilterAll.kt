package io.vyne.models.functions.stdlib.collections

import arrow.core.getOrHandle
import io.vyne.models.EvaluatedExpression
import io.vyne.models.EvaluationValueSupplier
import io.vyne.models.TypedCollection
import io.vyne.models.TypedInstance
import io.vyne.models.functions.FunctionResultCacheKey
import io.vyne.models.functions.NamedFunctionInvoker
import io.vyne.schemas.Schema
import io.vyne.schemas.Type
import lang.taxi.functions.FunctionAccessor
import lang.taxi.types.FormatsAndZoneOffset
import lang.taxi.types.QualifiedName

object FilterAll : NamedFunctionInvoker, CollectionFilteringFunction()  {
   override val functionName: QualifiedName = lang.taxi.functions.stdlib.FilterAll.name
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
      return applyFilter(inputValues, schema, returnType, function, objectFactory, rawMessageBeingParsed)
         .map {
            if (it.isEmpty()) {
                TypedCollection.empty(returnType)
            } else {
                TypedCollection.from(it, source = EvaluatedExpression(function.asTaxi(), inputValues))
            }
            }

         .getOrHandle { it }
   }
}
