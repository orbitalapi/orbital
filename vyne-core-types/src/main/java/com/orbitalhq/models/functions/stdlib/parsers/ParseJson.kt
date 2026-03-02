package com.orbitalhq.models.functions.stdlib.parsers

import arrow.core.flatMap
import arrow.core.getOrElse
import com.orbitalhq.models.EvaluatedExpression
import com.orbitalhq.models.EvaluationValueSupplier
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.models.functions.FunctionResultCacheKey
import com.orbitalhq.models.functions.NullSafeInvoker
import com.orbitalhq.models.functions.stdlib.valueAsOrError
import com.orbitalhq.schemas.Schema
import com.orbitalhq.schemas.Type
import lang.taxi.functions.FunctionAccessor
import lang.taxi.types.FormatsAndZoneOffset
import lang.taxi.types.QualifiedName

object ParseJson : NullSafeInvoker() {
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
      val parsedOrError = inputValues[0].valueAsOrError<String>(returnType, inputValues, function)
         .flatMap { rawJson ->
            inputValues[1].valueAsOrError<Type>(returnType, inputValues, function).map { targetType ->
               TypedInstance.from(targetType, rawJson, schema, source = EvaluatedExpression(function.asTaxi(), inputValues))
            }
         }
      return parsedOrError.getOrElse { it }
   }

   override val functionName: QualifiedName = lang.taxi.functions.stdlib.ParseJson.name
}
