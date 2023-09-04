package com.orbitalhq.expressions

import com.orbitalhq.formulas.Calculator
import com.orbitalhq.formulas.CalculatorRegistry
import com.orbitalhq.models.EvaluatedExpression
import com.orbitalhq.models.FailedEvaluatedExpression
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.models.TypedNull
import com.orbitalhq.schemas.Schema
import com.orbitalhq.schemas.Type
import lang.taxi.types.FormulaOperator

object OperatorExpressionCalculator {
   private val calculatorRegistry: CalculatorRegistry = CalculatorRegistry()
   fun calculate(
      lhs: TypedInstance,
      rhs: TypedInstance,
      operator: FormulaOperator,
      requestedOutputType: Type,
      expressionText: String,
      schema: Schema
   ): TypedInstance {
      val inputTypes = listOf(lhs.type, rhs.type)
      val inputs = listOf(lhs, rhs)
      val calculator = calculatorRegistry.getCalculator(operator, inputTypes)
         ?: return TypedNull.create(
            requestedOutputType,
            FailedEvaluatedExpression(
               expressionText, inputs,
               "No calculator exists for type ${inputTypes.joinToString { (it.basePrimitiveTypeName ?: it.name).fullyQualifiedName }}"
            )
         )
      val nulls = inputs.filterIsInstance<TypedNull>()
      if (!calculator.supportsNullValues && nulls.isNotEmpty()) {
         return failBecauseOfTypedNulls(
            nulls,
            calculator,
            requestedOutputType,
            expressionText,
            inputs
         )
      }
      return try {
         val evaluated = calculator.calculate(operator, inputs)
         TypedInstance.from(
            requestedOutputType,
            evaluated,
            schema,
            source = EvaluatedExpression(expressionText, inputs)
         )
      } catch (e:Exception) {
         TypedNull.create(
            requestedOutputType,
            source = FailedEvaluatedExpression(
               expressionText,
               inputs,
               errorMessage = e.message ?: "${e::class.simpleName} - No error provided"
            )
         )
      }
   }

   private fun failBecauseOfTypedNulls(
      nulls: List<TypedNull>,
      calculator: Calculator,
      requestedOutputType: Type,
      expressionText: String,
      inputs: List<TypedInstance>
   ): TypedNull {
      val message = nulls.joinToString(
         separator = "\n",
         prefix = "${calculator::class.simpleName} doesn't support nulls, but some inputs were null: \n"
      ) { typedNull ->
         val cause = if (typedNull.source is FailedEvaluatedExpression) {
            typedNull.source.errorMessage
         } else {
            typedNull.source.toString()
         }
         "Type ${typedNull.type.qualifiedName.shortDisplayName} was null - $cause"
      }
      val failedAttempts = nulls.map { it.source }
      return TypedNull.create(
         requestedOutputType,
         FailedEvaluatedExpression(
            expressionText,
            inputs,
            message,
            failedAttempts = failedAttempts
         )
      )
   }
}
