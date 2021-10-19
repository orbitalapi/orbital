package io.vyne.formulas

import io.vyne.schemas.Schema
import io.vyne.schemas.Type
import lang.taxi.types.FormulaOperator
import lang.taxi.types.PrimitiveType

class LogicCalculator : Calculator {
   companion object {
      val supportedOperations = setOf(
         FormulaOperator.LogicalAnd,
         FormulaOperator.LogicalOr,
      )
   }

   override fun canCalculate(operator: FormulaOperator, types: List<Type>): Boolean {
      return supportedOperations.contains(operator) && types.all { it.taxiType.basePrimitive != null && it.taxiType.basePrimitive!! == PrimitiveType.BOOLEAN }
   }


   override fun calculate(operator: FormulaOperator, values: List<Any?>): Any? {
      val notNullValues = values.filterNotNull()
         .filterIsInstance<Boolean>()
      require(notNullValues.size == 2) { "LogicCalculator expects 2 inputs, got ${values.size} (${notNullValues.size} after filtering nulls)" }
      val (a,b) = notNullValues
      return when (operator) {
         FormulaOperator.LogicalAnd -> a && b
         FormulaOperator.LogicalOr -> a || b
         else -> error("Unexpected operator : $operator")
      }
   }

   override fun getReturnType(operator: FormulaOperator, types: List<Type>, schema: Schema): Type {
      return schema.type(PrimitiveType.BOOLEAN)
   }
}
