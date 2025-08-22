package com.orbitalhq.models.functions.stdlib.collections

import com.orbitalhq.models.EvaluatedExpression
import com.orbitalhq.models.EvaluationValueSupplier
import com.orbitalhq.models.TypedCollection
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.models.functions.FunctionResultCacheKey
import com.orbitalhq.models.functions.NamedFunctionInvoker
import com.orbitalhq.models.functions.NullSafeInvoker
import com.orbitalhq.schemas.Schema
import com.orbitalhq.schemas.Type
import lang.taxi.functions.FunctionAccessor
import lang.taxi.types.FormatsAndZoneOffset
import lang.taxi.types.QualifiedName

object JoinToString : NullSafeInvoker() {
   override val functionName: QualifiedName = lang.taxi.functions.stdlib.JoinToString.name
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
      val sourceCollection = inputValues[0] as TypedCollection
      val seperator = inputValues[1].toRawObject()?.toString() ?: ","
      val prefix = inputValues[2].toRawObject()?.toString() ?: ""
      val postfix = inputValues[3]?.toRawObject()?.toString() ?: ""

      val sourceStrings = sourceCollection.mapNotNull { it.toRawObject()?.toString() }
      val result = sourceStrings.joinToString(seperator, prefix, postfix)

      return TypedInstance.from(returnType, result, schema, source = EvaluatedExpression(function.asTaxi(), inputValues))
   }
}
