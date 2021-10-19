package io.vyne.formulas

import io.vyne.schemas.Schema
import io.vyne.schemas.Type
import io.vyne.schemas.taxi.toVyneQualifiedName
import io.vyne.utils.log
import lang.taxi.types.FormulaOperator
import lang.taxi.types.PrimitiveType
import java.math.BigDecimal
import java.math.RoundingMode


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
   companion object {
      /**
       * This is an arbitary choice - we need to define SOME form of max precision
       * otherwise BigDecimal defaults to 0.
       * The actual max precision defined by the JVM is approx  2,147,483,647 digits.
       */

      const val MAX_PRECISION = 10_000
      val supportedOperations = setOf(
         FormulaOperator.Add,
         FormulaOperator.Subtract,
         FormulaOperator.Divide,
         FormulaOperator.Multiply
      )
   }

   override fun canCalculate(operator: FormulaOperator, types: List<Type>): Boolean {
      return supportedOperations.contains(operator) && types.all {
         it.taxiType.basePrimitive != null && PrimitiveType.NUMBER_TYPES.contains(
            it.taxiType.basePrimitive!!
         )
      }
   }

   override fun getReturnType(operator: FormulaOperator, types: List<Type>, schema: Schema): Type {
      fun hasInputOfType(primitiveType: PrimitiveType): Boolean {
         return types.any { it.basePrimitiveTypeName == primitiveType.toVyneQualifiedName() }
      }

      val returnType = when {
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
            is Int -> acc.toBigDecimal().divide((next as Int).toBigDecimal(), MAX_PRECISION, RoundingMode.HALF_UP)
               .stripTrailingZeros()
            is Double -> acc / next as Double
            is Float -> acc / next as Float
            is BigDecimal -> acc.divide(next as BigDecimal, MAX_PRECISION, RoundingMode.HALF_UP).stripTrailingZeros()
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
