package com.orbitalhq.models.functions.stdlib.dates

import com.orbitalhq.models.EvaluatedExpression
import com.orbitalhq.models.EvaluationValueSupplier
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.models.TypedValue
import com.orbitalhq.models.functions.FunctionResultCacheKey
import com.orbitalhq.models.functions.NullSafeInvoker
import com.orbitalhq.models.functions.functionFailed
import com.orbitalhq.schemas.Schema
import com.orbitalhq.schemas.Type
import lang.taxi.functions.FunctionAccessor
import lang.taxi.functions.stdlib.ParseDate
import lang.taxi.types.FormatsAndZoneOffset
import lang.taxi.types.PrimitiveType
import lang.taxi.types.QualifiedName

object ParseDate : NullSafeInvoker() {
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
      val baseDateType = returnType.basePrimitiveTypeName
         ?: return functionFailed(
            returnType,
            function,
            inputValues,
            "Type ${returnType.name.shortDisplayName} does not inherit from a date type"
         )

      val valueToParse = inputValues[0]
      val format = returnTypeFormat ?: returnType.formatAndZoneOffset
      val result = when (baseDateType.fullyQualifiedName) {
         PrimitiveType.INSTANT.qualifiedName,
         PrimitiveType.DATE_TIME.qualifiedName,
         PrimitiveType.LOCAL_DATE.qualifiedName -> {
            // Date parsing is already handled in the TypedInstance.from() method,
            // so just defer to that.
            TypedInstance.from(
               returnType,
               valueToParse.value,
               schema,
               source = EvaluatedExpression(
                  function.asTaxi(),
                  inputValues,
               ),
               format = format
            )
         }

         else -> functionFailed(
            returnType,
            function,
            inputValues,
            "Type ${returnType.name.shortDisplayName} is not parsable as a date type"
         )
      }
      return result
   }

   override val functionName: QualifiedName = ParseDate.name
}
