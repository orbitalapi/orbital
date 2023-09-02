package com.orbitalhq.formulas

import com.orbitalhq.schemas.Schema
import com.orbitalhq.schemas.Type
import lang.taxi.types.FormulaOperator
import lang.taxi.types.PrimitiveType


internal class StringCalculator : Calculator {
   override fun canCalculate(operator: FormulaOperator, types: List<Type>): Boolean {
      return types.all { it.taxiType.basePrimitive == PrimitiveType.STRING }
         && operator == FormulaOperator.Add
   }

   override fun getReturnType(operator: FormulaOperator, types: List<Type>, schema: Schema): Type {
      return schema.type(PrimitiveType.STRING)
   }

   override fun doCalculate(operator: FormulaOperator, values: List<Any?>): Any? {
      return if (values.any { it == null }) {
         null
      } else { values.reduce { acc, next -> acc as String + next as String }
      }
   }

}
