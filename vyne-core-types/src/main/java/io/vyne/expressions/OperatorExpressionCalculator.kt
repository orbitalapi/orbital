package io.vyne.expressions

import io.vyne.formulas.Calculator
import io.vyne.formulas.CalculatorRegistry
import io.vyne.models.EvaluatedExpression
import io.vyne.models.FailedEvaluatedExpression
import io.vyne.models.TypedInstance
import io.vyne.models.TypedNull
import io.vyne.schemas.Schema
import io.vyne.schemas.Type
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
      val evaluated = calculator.calculate(operator, inputs.map { it.value })
      return TypedInstance.from(
         requestedOutputType,
         evaluated,
         schema,
         source = EvaluatedExpression(expressionText, inputs)
      )
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
