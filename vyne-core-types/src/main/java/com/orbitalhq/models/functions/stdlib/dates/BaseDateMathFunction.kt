package com.orbitalhq.models.functions.stdlib.dates

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.orbitalhq.models.EvaluatedExpression
import com.orbitalhq.models.EvaluationValueSupplier
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.models.functions.FunctionResultCacheKey
import com.orbitalhq.models.functions.NullSafeInvoker
import com.orbitalhq.models.functions.functionFailed
import com.orbitalhq.schemas.Schema
import com.orbitalhq.schemas.Type
import com.orbitalhq.utils.get
import lang.taxi.functions.FunctionAccessor
import lang.taxi.functions.stdlib.AddDays
import lang.taxi.functions.stdlib.AddMinutes
import lang.taxi.functions.stdlib.AddSeconds
import lang.taxi.types.FormatsAndZoneOffset
import lang.taxi.types.QualifiedName
import java.time.Instant
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.time.temporal.Temporal
import java.time.temporal.TemporalAmount
import java.time.temporal.TemporalUnit

abstract class BaseDateMathFunction(private val unit: TemporalUnit) : NullSafeInvoker() {
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
      fun failed(message: String): TypedInstance {
         return functionFailed(returnType, function, inputValues, message)
      }
      if (inputValues.size != 2) {
         return failed("Expected 2 arguments, but got ${inputValues.size}")
      }
      val temporal = inputValues[0].value!!
      if (temporal !is Temporal) {
         return failed(
            "Expected a type related to date/time, but got ${inputValues[0].type.name.shortDisplayName}"
         )
      }
      val amount = inputValues[1].value!!
      if (amount !is Number) {
         return failed(
            "Expected a numeric type, but got ${inputValues[1].type.name.shortDisplayName}"
         )
      }

      return when (val result = calculate(inputValues[0], amount.toLong())) {
         is Either.Left -> failed(result.value)
         is Either.Right -> TypedInstance.from(
            returnType, result.value, schema, source = EvaluatedExpression(
               function.asTaxi(), inputValues
            )
         )
      }
   }

   protected open fun calculate(date: TypedInstance, amount: Long): Either<String, Temporal> {
      val temporal = date.value as Temporal
      return try {
         temporal.plus(amount, unit).right()
      } catch (e: Exception) {
         "${functionName.typeName} not supported for date type ${date.type.name.shortDisplayName}".left()
      }
   }

}

object AddMinutes : BaseDateMathFunction(ChronoUnit.MINUTES) {
   override val functionName: QualifiedName = AddMinutes.name
}

object AddDays : BaseDateMathFunction(ChronoUnit.DAYS) {
   override val functionName: QualifiedName = AddDays.name
}

object AddSeconds : BaseDateMathFunction(ChronoUnit.SECONDS) {
   override val functionName: QualifiedName = AddSeconds.name
}
