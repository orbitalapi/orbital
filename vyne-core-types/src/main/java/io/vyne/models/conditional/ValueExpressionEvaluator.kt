package io.vyne.models.conditional

import io.vyne.models.DefinedInSchema
import io.vyne.models.TypedInstance
import io.vyne.models.TypedObjectFactory
import io.vyne.models.TypedValue
import io.vyne.schemas.Type
import io.vyne.utils.log
import lang.taxi.services.operations.constraints.EnumValueExpression
import lang.taxi.types.LiteralCaseMatchExpression
import lang.taxi.types.ReferenceCaseMatchExpression
import lang.taxi.types.ValueExpression

class ValueExpressionEvaluator(private val factory: TypedObjectFactory) {
   fun evaluateExpression(matchExpression: ValueExpression, type: Type): TypedInstance {
      return when (matchExpression) {
         is ReferenceCaseMatchExpression -> factory.getValue(matchExpression.reference)
         // Note - I'm assuming the literal value is the same type as what we're comparing to.
         // Reasonable for now, but suspect subtypes etc may cause complexity here I haven't considered
         is LiteralCaseMatchExpression -> TypedInstance.from(type, matchExpression.value, factory.schema, source = DefinedInSchema)
         is EnumValueExpression -> TypedValue.from(type, matchExpression.enumValue.name,true,source = DefinedInSchema)
         else -> {
            log().warn("Unexpected match Expression $matchExpression")
            TODO()
         }
      }
   }

   fun expressionEvaluatesEqualTo(valueExpression: ValueExpression, type: Type, valueToCompareTo: TypedInstance): Boolean {
      val expressionValue = evaluateExpression(valueExpression, type)
      return valueToCompareTo.valueEquals(expressionValue)
   }

}
