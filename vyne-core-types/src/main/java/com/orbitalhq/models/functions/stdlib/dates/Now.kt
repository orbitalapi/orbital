package com.orbitalhq.models.functions.stdlib.dates

import com.orbitalhq.models.EvaluatedExpression
import com.orbitalhq.models.EvaluationValueSupplier
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.models.functions.FunctionResultCacheKey
import com.orbitalhq.models.functions.NamedFunctionInvoker
import com.orbitalhq.models.functions.functionFailed
import com.orbitalhq.schemas.Schema
import com.orbitalhq.schemas.Type
import lang.taxi.functions.FunctionAccessor
import lang.taxi.functions.stdlib.Now
import lang.taxi.types.FormatsAndZoneOffset
import lang.taxi.types.PrimitiveType
import lang.taxi.types.QualifiedName
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

object Now : NamedFunctionInvoker {
   override val functionName: QualifiedName = Now.name

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
      val now = when (returnType.basePrimitiveTypeName?.fullyQualifiedName) {
         PrimitiveType.INSTANT.qualifiedName -> Instant.now()
         PrimitiveType.LOCAL_DATE.qualifiedName -> LocalDate.now()
         PrimitiveType.DATE_TIME.qualifiedName -> LocalDateTime.now()
         PrimitiveType.TIME.qualifiedName -> LocalTime.now()
         else -> return functionFailed(
            returnType,
            function,
            inputValues,
            "now() is not supported for type ${returnType.name.shortDisplayName}"
         )
      }

      return TypedInstance.from(
         returnType, now, schema, source = EvaluatedExpression(
            function.asTaxi(),
            inputValues
         )
      )
   }
}
