package io.vyne.formulas

import io.vyne.schemas.Schema
import io.vyne.schemas.Type
import lang.taxi.types.FormulaOperator
import lang.taxi.types.PrimitiveType

class ComparableCalculator : Calculator {
   companion object {
      private val supportedSymbols = setOf(
         FormulaOperator.GreaterThan,
         FormulaOperator.GreaterThanOrEqual,
         FormulaOperator.LessThan,
         FormulaOperator.LessThanOrEqual,
         FormulaOperator.NotEqual,
         FormulaOperator.Equal
      )
   }

   override fun canCalculate(operator: FormulaOperator, types: List<Type>): Boolean {
      return supportedSymbols.contains(operator)
   }

   override fun calculate(operator: FormulaOperator, values: List<Any?>): Any? {
      val notNullValues = values.filterNotNull()
         .filterIsInstance<Comparable<Any>>()
      require(notNullValues.size == 2) { "ComparableCalculator expects 2 inputs, got ${values.size} (${notNullValues.size} after filtering nulls)" }
      val (a: Comparable<Any>, b: Comparable<Any>) = notNullValues
      val result = when (operator) {
         FormulaOperator.GreaterThan -> a > b
         FormulaOperator.GreaterThanOrEqual -> a >= b
         FormulaOperator.LessThan -> a <= b
         FormulaOperator.LessThanOrEqual -> a < b
         FormulaOperator.Equal -> a == b
         FormulaOperator.NotEqual -> a != b
         else -> error("Unexpected symbol in comparator operator: ${operator.symbol}")
      }
      return result
   }

   override fun getReturnType(operator: FormulaOperator, types: List<Type>, schema: Schema): Type {
      return schema.type(PrimitiveType.BOOLEAN)
   }
}
