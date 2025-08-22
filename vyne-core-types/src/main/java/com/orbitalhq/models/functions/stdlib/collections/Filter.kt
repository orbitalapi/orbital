package com.orbitalhq.models.functions.stdlib.collections

import arrow.core.getOrHandle
import com.fasterxml.jackson.databind.ser.std.NullSerializer
import com.orbitalhq.models.EvaluatedExpression
import com.orbitalhq.models.EvaluationValueSupplier
import com.orbitalhq.models.TypedCollection
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.models.TypedNull
import com.orbitalhq.models.functions.FunctionResultCacheKey
import com.orbitalhq.models.functions.NamedFunctionInvoker
import com.orbitalhq.models.functions.NullSafeInvoker
import com.orbitalhq.schemas.Schema
import com.orbitalhq.schemas.Type
import lang.taxi.functions.FunctionAccessor
import lang.taxi.types.FormatsAndZoneOffset
import lang.taxi.types.QualifiedName

object Filter : CollectionFilteringFunction() {
   override val functionName: QualifiedName = lang.taxi.functions.stdlib.Filter.name
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
      // MP: 2024-12-18 -- this seems wrong, but
      // needed for a customer fix.
      // We should test coalescing instead in the query layer.
      // See ticket ORB-851 for specifics
      if (inputValues[0] is TypedNull) {
         return TypedCollection.empty(returnType)
      }
      val result = applyFilter(inputValues, schema, returnType, function, thisScopeValueSupplier, rawMessageBeingParsed)
         .map {
            if (it.isEmpty()) {
               TypedCollection.empty(returnType)
            } else {
               TypedCollection.from(it, source = EvaluatedExpression(function.asTaxi(), inputValues))
            }
         }

         .getOrHandle { it }
      return result
   }
}

