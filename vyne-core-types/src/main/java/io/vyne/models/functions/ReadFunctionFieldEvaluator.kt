package io.vyne.models.functions

import io.vyne.models.TypedInstance
import io.vyne.schemas.AttributeName
import io.vyne.schemas.Type
import lang.taxi.accessors.ReadFunction
import lang.taxi.accessors.ReadFunctionFieldAccessor

@Deprecated("Use functions instead")
class ReadFunctionFieldEvaluator {
   fun evaluate(readCondition: ReadFunctionFieldAccessor, targetType: Type): TypedInstance {
      return evaluate(readCondition, attributeName = null, targetType = targetType)
   }
   fun evaluate(readCondition: ReadFunctionFieldAccessor, attributeName: AttributeName?, targetType: Type): TypedInstance {
      return when (readCondition.readFunction) {
         ReadFunction.CONCAT -> performConcat(readCondition, attributeName, targetType)
         else -> error("Unhandled type of readCondition: ${readCondition::class.simpleName}")
      }
   }

   private fun performConcat(readCondition: ReadFunctionFieldAccessor, attributeName: AttributeName?, targetType: Type): TypedInstance {
      TODO("Not yet implemented")
   }
}
