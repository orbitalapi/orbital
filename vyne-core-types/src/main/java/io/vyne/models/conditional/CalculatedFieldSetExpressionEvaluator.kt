package io.vyne.models.conditional

import io.vyne.formulas.CalculatorRegistry
import io.vyne.models.EvaluatedExpression
import io.vyne.models.EvaluationValueSupplier
import io.vyne.models.FailedEvaluatedExpression
import io.vyne.models.TypedInstance
import io.vyne.models.TypedNull
import io.vyne.models.safeTaxi
import io.vyne.schemas.AttributeName
import io.vyne.schemas.Schema
import io.vyne.schemas.Type
import io.vyne.utils.log
import lang.taxi.types.CalculatedFieldSetExpression

@Deprecated("use expressions instead")
class CalculatedFieldSetExpressionEvaluator(
   private val factory: EvaluationValueSupplier,
   private val schema:Schema,
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
         val value = calculator.calculate(readCondition.operator, operandValues)
         TypedInstance.from(
            targetType,
            value,
            schema,
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



