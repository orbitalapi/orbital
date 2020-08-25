package io.vyne.models.conditional

import io.vyne.models.TypedInstance
import io.vyne.models.TypedObjectFactory
import io.vyne.schemas.AttributeName
import io.vyne.schemas.Type
import lang.taxi.types.CalculatedFieldSetExpression
import lang.taxi.types.FieldSetExpression
import lang.taxi.types.UnaryCalculatedFieldSetExpression
import lang.taxi.types.WhenFieldSetCondition

class ConditionalFieldSetEvaluator(private val factory: TypedObjectFactory) {
   private val whenEvaluator = WhenFieldSetConditionEvaluator(factory)
   private val calculatedFieldEvaluator = CalculatedFieldSetExpressionEvaluator(factory)

   fun evaluate(readCondition: FieldSetExpression, targetType: Type): TypedInstance {
      return evaluate(readCondition, attributeName = null, targetType = targetType)
   }
   fun evaluate(readCondition: FieldSetExpression, attributeName: AttributeName?, targetType: Type): TypedInstance {
      return when (readCondition) {
         is WhenFieldSetCondition -> whenEvaluator.evaluate(readCondition, attributeName, targetType)
         is CalculatedFieldSetExpression -> calculatedFieldEvaluator.evaluate(readCondition, attributeName, targetType)
         is UnaryCalculatedFieldSetExpression -> calculatedFieldEvaluator.evaluate(readCondition, attributeName, targetType)
         else -> error("Unhandled type of readCondition: ${readCondition::class.simpleName}")
      }
   }
}
