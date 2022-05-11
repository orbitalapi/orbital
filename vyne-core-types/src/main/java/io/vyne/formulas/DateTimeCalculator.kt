package io.vyne.formulas

import io.vyne.schemas.Schema
import io.vyne.schemas.Type
import lang.taxi.types.FormulaOperator
import lang.taxi.types.PrimitiveType
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset


internal class DateTimeCalculator : Calculator {
   override fun canCalculate(operator: FormulaOperator, types: List<Type>): Boolean {
      return types.size == 2 &&
         types[0].taxiType.basePrimitive == PrimitiveType.LOCAL_DATE &&
         types[1].taxiType.basePrimitive == PrimitiveType.TIME &&
         operator == FormulaOperator.Add
   }

   override fun getReturnType(operator: FormulaOperator, types: List<Type>, schema: Schema): Type {
      return schema.type(PrimitiveType.INSTANT)
   }

   override fun calculate(operator: FormulaOperator, values: List<Any?>): Any? {
      if (values.any { it == null }) {
         return null
      }
      val date = values[0] as LocalDate
      val time = values[1] as LocalTime
      // This is a problem.
      // Neither Date nor Time contains any Zone data.  However, the expected
      // result of the concatenation is (currently) an Instant.
      // Project using UTC for now.  In practice, we need to retool this to
      // return a LocalDateTime
      return date.atTime(time).toInstant(ZoneOffset.UTC)
   }

}

