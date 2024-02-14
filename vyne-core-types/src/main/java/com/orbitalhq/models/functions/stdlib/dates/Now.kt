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
import lang.taxi.functions.stdlib.CurrentDate
import lang.taxi.functions.stdlib.CurrentDateTime
import lang.taxi.functions.stdlib.CurrentTime
import lang.taxi.functions.stdlib.Now
import lang.taxi.types.FormatsAndZoneOffset
import lang.taxi.types.PrimitiveType
import lang.taxi.types.QualifiedName
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.Temporal

abstract class BaseCurrentTimeInvoker(private val valueProvider: () -> Temporal) : NamedFunctionInvoker {
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
      val now = valueProvider()
      return TypedInstance.from(
         returnType, now, schema, source = EvaluatedExpression(
            function.asTaxi(),
            inputValues
         )
      )
   }
}

object Now : BaseCurrentTimeInvoker(Instant::now) {
   override val functionName: QualifiedName = Now.name
}

object CurrentDate : BaseCurrentTimeInvoker(LocalDate::now) {
   override val functionName: QualifiedName = CurrentDate.name
}

object CurrentDateTime : BaseCurrentTimeInvoker(LocalDateTime::now) {
   override val functionName: QualifiedName = CurrentDateTime.name
}

object CurrentTime : BaseCurrentTimeInvoker(LocalTime::now) {
   override val functionName: QualifiedName = CurrentTime.name
}
