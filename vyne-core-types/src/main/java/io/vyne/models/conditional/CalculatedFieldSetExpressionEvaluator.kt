package io.vyne.models.conditional

import io.vyne.formulas.CalculatorRegistry
import io.vyne.models.Calculated
import io.vyne.models.DefinedInSchema
import io.vyne.models.TypedInstance
import io.vyne.models.TypedNull
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
      val value = calculator.calculate(readCondition.operator, operandValues.map { it.value })
      return TypedInstance.from(targetType, value, factory.schema, source = Calculated)
   }

//   fun evaluate(readCondition: UnaryCalculatedFieldSetExpression, attributeName: AttributeName?, targetType: Type): TypedInstance {
//      val operandTypedInstance = factory.getValue(readCondition.operand.fieldName)
//      val operandType = operandTypedInstance.type
//      val unaryCalculator = calculatorRegistry.getUnaryCalculator(readCondition.operator, operandType)
//         ?: error("Invalid unary calculated field $attributeName ${readCondition.operator}")
//      val opValue = operandTypedInstance.value
//      return if (opValue == null) {
//         TypedNull(operandType, source = DefinedInSchema)
//      } else {
//         val value = unaryCalculator.calculate(readCondition.operator, opValue, readCondition.literal)
//         TypedInstance.from(targetType, value, factory.schema, source = Calculated)
//      }
//   }
//
//   fun evaluate(readCondition: TerenaryFieldSetExpression, attributeName: AttributeName?, targetType: Type): TypedInstance {
//      val operandValues = listOf(
//         factory.getValue(readCondition.operand1.fieldName),
//         factory.getValue(readCondition.operand2.fieldName),
//         factory.getValue(readCondition.operand3.fieldName))
//      val calculator = calculatorRegistry.getTerenaryCalculator(readCondition.operator, operandValues.map { it.type }) ?: error("Invalid calculated field")
//      val value = calculator.calculate(readCondition.operator, operandValues.map { it.value!! }.plus(readCondition.literal))
//      return TypedInstance.from(targetType, value, factory.schema, source = Calculated)
//   }
}
