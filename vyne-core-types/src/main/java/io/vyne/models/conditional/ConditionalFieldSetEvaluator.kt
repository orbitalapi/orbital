package io.vyne.models.conditional

import io.vyne.models.TypedInstance
import io.vyne.models.TypedObjectFactory
import io.vyne.schemas.AttributeName
import io.vyne.schemas.Type
import lang.taxi.types.FieldSetCondition
import lang.taxi.types.WhenFieldSetCondition

class ConditionalFieldSetEvaluator(private val factory: TypedObjectFactory) {
   private val whenEvaluator = WhenFieldSetConditionEvaluator(factory)

   fun evaluate(readCondition: FieldSetCondition, attributeName: AttributeName, targetType: Type): TypedInstance {
      return when (readCondition) {
         is WhenFieldSetCondition -> whenEvaluator.evaluate(readCondition, attributeName, targetType)
         else -> error("Unhandled type of readCondition: ${readCondition::class.simpleName}")
      }
   }
}
