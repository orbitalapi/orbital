package io.vyne.expressions

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
      val evaluated = calculator.calculate(operator, inputs.map { it.value })
      return TypedInstance.from(
         requestedOutputType,
         evaluated,
         schema,
         source = EvaluatedExpression(expressionText, inputs)
      )
   }
}
