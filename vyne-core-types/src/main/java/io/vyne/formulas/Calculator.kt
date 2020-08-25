package io.vyne.formulas

import io.vyne.schemas.Type
import lang.taxi.types.Formula
import lang.taxi.types.FormulaOperator
import lang.taxi.types.PrimitiveType
import lang.taxi.types.UnaryFormulaOperator
import org.apache.commons.lang3.StringUtils
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset

interface Calculator {
   fun canCalculate(operator: FormulaOperator, types: List<Type>): Boolean
   fun calculate(operator: FormulaOperator, values: List<Any>): Any?
}

interface UnaryCalculator {
   fun canCalculate(operator: UnaryFormulaOperator, type: Type): Boolean
   fun calculate(operator: UnaryFormulaOperator, value: Any, literal: Any): Any?
}

class CalculatorRegistry(private val calculators: List<Calculator> = listOf(
   NumberCalculator(),
   StringCalculator(),
   DateTimeCalculator()
), private val unaryCalculators: List<UnaryCalculator> = listOf(LeftCalculator())) {
   fun getCalculator(operator: FormulaOperator, types: List<Type>): Calculator? {
      return calculators.firstOrNull { it.canCalculate(operator, types) }
   }

   fun getUnaryCalculator(operator: UnaryFormulaOperator, type: Type): UnaryCalculator? {
      return unaryCalculators.firstOrNull { it.canCalculate(operator, type)}
   }
}

internal class LeftCalculator: UnaryCalculator {
   override fun canCalculate(operator: UnaryFormulaOperator, type: Type): Boolean {
     return (
        type.taxiType.basePrimitive == PrimitiveType.STRING
           &&
           operator == UnaryFormulaOperator.Left)
   }

   override fun calculate(operator: UnaryFormulaOperator, value: Any, literal: Any): Any? {
      return if (literal.toString().toIntOrNull() == null) {
         value
      } else {
         StringUtils.left(value.toString(), literal.toString().toInt())
      }
   }
}

internal class StringCalculator : Calculator {
   override fun canCalculate(operator: FormulaOperator, types: List<Type>): Boolean {
      return types.all { it.taxiType.basePrimitive == PrimitiveType.STRING }
         && operator == FormulaOperator.Add
   }

   override fun calculate(operator: FormulaOperator, values: List<Any>): Any? {
      return values.reduce { acc, next -> acc as String + next as String }
   }

}

internal class DateTimeCalculator : Calculator {
   override fun canCalculate(operator: FormulaOperator, types: List<Type>): Boolean {
      return types.size == 2 &&
         types[0].taxiType.basePrimitive == PrimitiveType.LOCAL_DATE &&
         types[1].taxiType.basePrimitive == PrimitiveType.TIME &&
         operator == FormulaOperator.Add
   }

   override fun calculate(operator: FormulaOperator, values: List<Any>): Any? {
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

/**
 * A simple number calculator, which applies basic operations to two operands.
 *
 * Current restriction is that both must be the same subtype of Number, however the
 * main reason for that restriction is laziness of typing, rather than anything more technical.
 *
 * I do wonder which has taken more typing, the implementation that I've omitted, or this explanation
 * about why I've omitted it.  Future generations will know.
 */
internal class NumberCalculator : Calculator {
   override fun canCalculate(operator: FormulaOperator, types: List<Type>): Boolean {
      return types.all { it.taxiType.basePrimitive != null && PrimitiveType.NUMBER_TYPES.contains(it.taxiType.basePrimitive!!) }
   }

   override fun calculate(operator: FormulaOperator, values: List<Any>): Any? {
      // I'm being lazy here - we can add support for cross-type operations later,
      // but it's just a huge amount of typing to cover all the possible scenarios
      val numberTypes = values.map { it::class.java }.distinct()
      if (numberTypes.size > 1) {
         error("Numeric formulas with differing number types is not yet supported - found ${numberTypes.joinToString { it.simpleName }}")
      }

      return when (operator) {
         FormulaOperator.Add -> addNumbers(values)
         FormulaOperator.Subtract -> subtractNumbers(values)
         FormulaOperator.Multiply -> multipleNumbers(values)
         FormulaOperator.Divide -> divideNumbers(values)
      }
   }

   private fun divideNumbers(values: List<Any>): Any? {
      return values.reduce { acc, next ->
         when (acc) {
            is Int -> acc / next as Int
            is Double -> acc / next as Double
            is Float -> acc / next as Float
            is BigDecimal -> acc.divide(next as BigDecimal)
            is Long -> acc / next as Long
            is Short -> acc / next as Short
            else -> error("Unsupported number type: ${acc::class.java.simpleName}")
         }
      }
   }

   private fun multipleNumbers(values: List<Any>): Any? {
      return values.reduce { acc, next ->
         when (acc) {
            is Int -> acc * next as Int
            is Double -> acc * next as Double
            is Float -> acc * next as Float
            is BigDecimal -> acc.multiply(next as BigDecimal)
            is Long -> acc * next as Long
            is Short -> acc * next as Short
            else -> error("Unsupported number type: ${acc::class.java.simpleName}")
         }
      }
   }

   private fun subtractNumbers(values: List<Any>): Any? {
      return values.reduce { acc, next ->
         when (acc) {
            is Int -> acc - next as Int
            is Double -> acc - next as Double
            is Float -> acc - next as Float
            is BigDecimal -> acc.subtract(next as BigDecimal)
            is Long -> acc - next as Long
            is Short -> acc - next as Short
            else -> error("Unsupported number type: ${acc::class.java.simpleName}")
         }
      }
   }

   private fun addNumbers(values: List<Any>): Any? {
      return values.reduce { acc, next ->
         when (acc) {
            is Int -> acc + next as Int
            is Double -> acc + next as Double
            is Float -> acc + next as Float
            is BigDecimal -> acc.add(next as BigDecimal)
            is Long -> acc + next as Long
            is Short -> acc + next as Short
            else -> error("Unsupported number type: ${acc::class.java.simpleName}")
         }
      }
   }
}

