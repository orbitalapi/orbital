package io.vyne.query.formulas

import lang.taxi.types.Formula
import lang.taxi.types.FormulaOperator
import java.math.BigDecimal

interface Calculator {
   fun canCalculate(operator: FormulaOperator): Boolean
   fun calculate(operands: List<Any>): Any?
}

class CalculatorRegistry(private val calculators: List<Calculator> = listOf(Multiplication())) {
   fun calculate(operator: FormulaOperator, operands: List<Any>): Any? {
      return calculators.firstOrNull { it.canCalculate(operator) }?.let { it.calculate(operands) }
   }
}

internal class Multiplication: Calculator {
   override fun canCalculate(operator: FormulaOperator) = operator == FormulaOperator.Multiply

   override fun calculate(operands: List<Any>): Any? {
      return operands.reduce { acc, number -> acc * number }
   }

   operator fun Any.times(other: Any): Any = when (this) {
      is Int ->  this.times(other as Int)
      is Double -> this.times(other as Double)
      is Float -> this.times(other as Float)
      is BigDecimal -> this.multiply(other as BigDecimal)
      is Long -> this.times(other as Long)
      is Short -> this.times(other as Short)
      else -> throw RuntimeException("unsupported type")
   }

}

