package io.vyne.models.conditional

import io.vyne.formulas.CalculatorRegistry
import io.vyne.models.Calculated
import io.vyne.models.TypedInstance
import io.vyne.models.TypedObjectFactory
import io.vyne.schemas.AttributeName
import io.vyne.schemas.Type
import lang.taxi.types.CalculatedFieldSetExpression

class CalculatedFieldSetExpressionEvaluator(
   private val factory: TypedObjectFactory,
   private val calculatorRegistry: CalculatorRegistry = CalculatorRegistry()) {
   fun evaluate(readCondition: CalculatedFieldSetExpression, attributeName: AttributeName?, targetType: Type): TypedInstance {
      val operandValues = listOf(factory.getValue(readCondition.operand1.fieldName), factory.getValue(readCondition.operand2.fieldName))
      val calculator = calculatorRegistry.getCalculator(readCondition.operator, operandValues.map { it.type }) ?: error("Invalid calculated field")
      val value = calculator.calculate(readCondition.operator, operandValues.map { it.value!! })
      return TypedInstance.from(targetType, value, factory.schema, source = Calculated)
   }
}
