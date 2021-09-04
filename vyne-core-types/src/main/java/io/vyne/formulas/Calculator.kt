package io.vyne.formulas

import io.vyne.schemas.Schema
import io.vyne.schemas.Type
import io.vyne.schemas.taxi.toVyneQualifiedName
import io.vyne.utils.log
import lang.taxi.types.FormulaOperator
import lang.taxi.types.PrimitiveType
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset

interface Calculator {
   val supportsNullValues:Boolean
      get() {
         return false
      }
   fun canCalculate(operator: FormulaOperator, types: List<Type>): Boolean
   fun calculate(operator: FormulaOperator, values: List<Any?>): Any?
   fun getReturnType(operator: FormulaOperator, types: List<Type>, schema: Schema):Type
}

class CalculatorRegistry(private val calculators: List<Calculator> = listOf(NumberCalculator(), StringCalculator(), DateTimeCalculator(), CoalesceCalculator())) {
   fun getCalculator(operator: FormulaOperator, types: List<Type>): Calculator? {
      return calculators.firstOrNull { it.canCalculate(operator, types) }
   }
}
//
//internal class LeftCalculator : UnaryCalculator {
//   override fun canCalculate(operator: UnaryFormulaOperator, type: Type): Boolean {
//      return (
//         type.taxiType.basePrimitive == PrimitiveType.STRING
//            &&
//            operator == UnaryFormulaOperator.Left)
//   }
//
//   override fun calculate(operator: UnaryFormulaOperator, value: Any, literal: Any): Any? {
//      return if (literal.toString().toIntOrNull() == null) {
//         value
//      } else {
//         StringUtils.left(value.toString(), literal.toString().toInt())
//      }
//   }
//}
//
//internal class Concat3 : TerenaryCalculator {
//   override fun canCalculate(operator: TerenaryFormulaOperator, types: List<Type>): Boolean {
//      return types.all { it.taxiType.basePrimitive == PrimitiveType.STRING }
//         && operator == TerenaryFormulaOperator.Concat3
//   }
//
//   override fun calculate(operator: TerenaryFormulaOperator, values: List<Any>): Any? {
//      return values.dropLast(1).joinToString(values.last().toString())
//   }
//}
//

// Tried replacing this, but we seem to be running a special discovery strategy
// for resolving type references.
// This needs to go away, but not until I have time to work out how to migrate
// that into the function architecture
@Deprecated("This needs to be replaced with a function invoker.")
internal class CoalesceCalculator: Calculator {
   override fun canCalculate(operator: FormulaOperator, types: List<Type>): Boolean {
      return operator == FormulaOperator.Coalesce
   }

   override fun calculate(operator: FormulaOperator, values: List<Any?>): Any? {
      val firstNonNullIfExists = values.firstOrNull { it != null }
      return firstNonNullIfExists
   }

   override fun getReturnType(operator: FormulaOperator, types: List<Type>, schema: Schema): Type {
      return types.first()
   }

}

internal class StringCalculator : Calculator {
   override fun canCalculate(operator: FormulaOperator, types: List<Type>): Boolean {
      return types.all { it.taxiType.basePrimitive == PrimitiveType.STRING }
         && operator == FormulaOperator.Add
   }

   override fun getReturnType(operator: FormulaOperator, types: List<Type>, schema: Schema): Type {
      return schema.type(PrimitiveType.STRING)
   }

   override fun calculate(operator: FormulaOperator, values: List<Any?>): Any? {
      return if (values.any { it == null }) {
         null
      } else { values.reduce { acc, next -> acc as String + next as String }
      }
   }

}

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

   override fun getReturnType(operator: FormulaOperator, types: List<Type>, schema: Schema): Type {
      fun hasInputOfType(primitiveType: PrimitiveType):Boolean {
         return types.any { it.basePrimitiveTypeName == primitiveType.toVyneQualifiedName() }
      }
      val returnType =  when {
         hasInputOfType(PrimitiveType.DOUBLE) -> PrimitiveType.DOUBLE
         hasInputOfType(PrimitiveType.DECIMAL) -> PrimitiveType.DECIMAL
         else -> PrimitiveType.INTEGER
      }
      return schema.type(returnType)
   }

   override fun calculate(operator: FormulaOperator, values: List<Any?>): Any? {
      if (values.any { it == null }) {
         return null
      }
      // I'm being lazy here - we can add support for cross-type operations later,
      // but it's just a huge amount of typing to cover all the possible scenarios
      val numberTypes = values.map { it!!::class.java }.distinct()
      if (numberTypes.size > 1) {
         log().warn("Multiple number types found: ${numberTypes.joinToString { it.simpleName }}. Current support for this is limited, so casting all to BigDecimal to proceed")
         val bigDecimals = values.map { BigDecimal(it.toString()) }
         return calculate(operator, bigDecimals)
//         error("Numeric formulas with differing number types is not yet supported - found ${numberTypes.joinToString { it.simpleName }}")
      }

      return when (operator) {
         FormulaOperator.Add -> addNumbers(values as List<Any>)
         FormulaOperator.Subtract -> subtractNumbers(values as List<Any>)
         FormulaOperator.Multiply -> multipleNumbers(values as List<Any>)
         FormulaOperator.Divide -> divideNumbers(values as List<Any>)
         else -> error("$operator not supported!")
      }
   }

   private fun divideNumbers(values: List<Any>): Any? {
      return values.reduce { acc, next ->
         when (acc) {
            is Int -> acc.toBigDecimal().divide((next as Int).toBigDecimal())
            is Double -> acc / next as Double
            is Float -> acc / next as Float
            is BigDecimal -> acc.divide(next as BigDecimal, 15, RoundingMode.HALF_UP).stripTrailingZeros()
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

