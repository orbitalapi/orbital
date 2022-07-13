package io.vyne.formulas

import io.vyne.models.TypedInstance
import io.vyne.schemas.Schema
import io.vyne.schemas.Type
import lang.taxi.types.FormulaOperator

interface Calculator {
   val supportsNullValues:Boolean
      get() {
         return false
      }
   fun canCalculate(operator: FormulaOperator, types: List<Type>): Boolean
   fun calculate(operator: FormulaOperator, values: List<TypedInstance>): Any? = doCalculate(operator, values.map { it.value })
   fun doCalculate(operator: FormulaOperator, values: List<Any?>): Any?
   fun getReturnType(operator: FormulaOperator, types: List<Type>, schema: Schema):Type
}

class CalculatorRegistry(private val calculators: List<Calculator> = listOf(LogicCalculator(), ComparableCalculator(), NumberCalculator(), StringCalculator(), DateTimeCalculator())) {
   fun getCalculator(operator: FormulaOperator, types: List<Type>): Calculator? {
      return calculators.firstOrNull { it.canCalculate(operator, types) }
   }
}


// Tried replacing this, but we seem to be running a special discovery strategy
// for resolving type references.
// This needs to go away, but not until I have time to work out how to migrate
// that into the function architecture
//@Deprecated("This needs to be replaced with a function invoker.")
//internal class CoalesceCalculator: Calculator {
//   override fun canCalculate(operator: FormulaOperator, types: List<Type>): Boolean {
//      return operator == FormulaOperator.Coalesce
//   }
//
//   override fun calculate(operator: FormulaOperator, values: List<Any?>): Any? {
//      val firstNonNullIfExists = values.firstOrNull { it != null }
//      return firstNonNullIfExists
//   }
//
//   override fun getReturnType(operator: FormulaOperator, types: List<Type>, schema: Schema): Type {
//      return types.first()
//   }
//
//}
