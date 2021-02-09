package io.vyne.models.conditional

import io.vyne.formulas.CalculatorRegistry
import io.vyne.models.EvaluatedExpression
import io.vyne.models.FailedEvaluatedExpression
import io.vyne.models.TypedInstance
import io.vyne.models.TypedNull
import io.vyne.models.TypedObjectFactory
import io.vyne.models.safeTaxi
import io.vyne.schemas.AttributeName
import io.vyne.schemas.Type
import io.vyne.utils.log
import lang.taxi.types.CalculatedFieldSetExpression

class CalculatedFieldSetExpressionEvaluator(
   private val factory: TypedObjectFactory,
   private val calculatorRegistry: CalculatorRegistry = CalculatorRegistry()
) {
   fun evaluate(
      readCondition: CalculatedFieldSetExpression,
      attributeName: AttributeName?,
      targetType: Type
   ): TypedInstance {
      val operandValues =
         listOf(factory.getValue(readCondition.operand1.fieldName), factory.getValue(readCondition.operand2.fieldName))
      val calculator = calculatorRegistry.getCalculator(readCondition.operator, operandValues.map { it.type }) ?: error(
         "Invalid calculated field"
      )
      return try {
         val value = calculator.calculate(readCondition.operator, operandValues.map { it.value })
         TypedInstance.from(
            targetType,
            value,
            factory.schema,
            source = EvaluatedExpression(readCondition.safeTaxi(), operandValues)
         )
      } catch (e: Exception) {
         val exceptionMessage = e.message ?: "A ${e::class.simpleName} exception was thrown"
         log().warn(
            "Failed to evaluation expression ${readCondition.safeTaxi()}.  Will return null, and continue processing.  Details are captured in the lineage for this value - $exceptionMessage",
            e
         )
         TypedNull.create(
            targetType,
            FailedEvaluatedExpression(readCondition.safeTaxi(), operandValues, exceptionMessage)
         )
      }


   }


}



