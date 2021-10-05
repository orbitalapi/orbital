package io.vyne.formulas

import io.vyne.schemas.Schema
import io.vyne.schemas.Type
import lang.taxi.types.FormulaOperator
import lang.taxi.types.PrimitiveType
import java.math.BigDecimal

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

   override val supportsNullValues: Boolean = true

   override fun calculate(operator: FormulaOperator, values: List<Any?>): Any {
      require(values.size == 2) { "ComparableCalculator expects 2 inputs, got ${values.size}" }
      if (values.any { it == null }) {
         return doNullComparison(operator, values)
      }
      val (a: Comparable<Any>, b: Comparable<Any>) = attemptCast(values as List<Comparable<Any>>)
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

   private fun attemptCast(values:List<Any>):Pair<Comparable<Any>,Comparable<Any>> {
      val (a,b) = values
      if (a::class == b::class) {
         @Suppress("UNCHECKED_CAST")
         return a as Comparable<Any> to b as Comparable<Any>
      }
      return when {
         a is Int && b is BigDecimal -> a.toBigDecimal() as Comparable<Any> to b as Comparable<Any>
         a is BigDecimal && b is Int -> a as Comparable<Any> to b.toBigDecimal() as Comparable<Any>
         else -> error("Comparable values are not of the same type (${a::class.simpleName} and ${b::class.simpleName}), and there's no way to cast them. ")
      }
   }

   private fun doNullComparison(operator: FormulaOperator, values: List<Any?>): Any {
      val (a:Any?, b:Any?) = values
      return when(operator) {
         FormulaOperator.Equal -> a == b
         FormulaOperator.NotEqual -> a != b
         else -> false // All others are falsey
      }
   }

   override fun getReturnType(operator: FormulaOperator, types: List<Type>, schema: Schema): Type {
      return schema.type(PrimitiveType.BOOLEAN)
   }
}
