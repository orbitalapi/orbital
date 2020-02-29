package io.vyne.models.conditional

import io.vyne.models.TypedInstance
import io.vyne.models.TypedObject
import io.vyne.models.TypedObjectFactory
import io.vyne.schemas.AttributeName
import io.vyne.schemas.Type
import io.vyne.schemas.asVyneTypeReference
import lang.taxi.types.*

class WhenFieldSetConditionEvaluator(private val factory: TypedObjectFactory) {
   fun evaluate(readCondition: WhenFieldSetCondition, attributeName: AttributeName, targetType:Type): TypedInstance {
      val selectorValue = evaluateSelector(readCondition.selectorExpression)
      val caseBlock = selectCaseBlock(selectorValue,readCondition)
      val assignmentExpression = caseBlock.getAssignmentFor(attributeName)
      val typedValue = evaluateExpression(assignmentExpression, targetType)
      return typedValue
   }

   private fun evaluateExpression(matchExpression: CaseFieldAssignmentExpression, type:Type): TypedInstance {
      val assignment = matchExpression.assignment
      return when(assignment) {
         is ScalarAccessorValueAssignment -> factory.readAccessor(type,assignment.accessor) // WTF? Why isn't the compiler working this out?
         is ReferenceAssignment -> factory.getValue(assignment.reference)
         is LiteralAssignment -> TypedInstance.from(type,assignment.value,factory.schema)
         is DestructuredAssignment -> {
            val resolvedAttributes = assignment.assignments.map { nestedAssignment ->
               val attributeType = factory.schema.type(type.attribute(nestedAssignment.fieldName).type)
               nestedAssignment.fieldName to evaluateExpression(nestedAssignment, attributeType)
            }.toMap()
            TypedObject.fromAttributes(type,resolvedAttributes,factory.schema)
         }
         else -> TODO()
      }
   }

   private fun selectCaseBlock(selectorValue: TypedInstance, readCondition: WhenFieldSetCondition): WhenCaseBlock {
      return readCondition.cases.firstOrNull { caseBlock ->
         val valueToCompare = evaluateExpression(caseBlock.matchExpression, selectorValue.type)
         selectorValue.valueEquals(valueToCompare)
      } ?: error("No matching cases found")

   }

   private fun evaluateExpression(matchExpression: WhenCaseMatchExpression, type: Type): TypedInstance {
      return when (matchExpression) {
         is ReferenceCaseMatchExpression -> factory.getValue(matchExpression.reference)
         // Note - I'm assuming the literal value is the same type as what we're comparing to.
         // Reasonable for now, but suspect subtypes etc may cause complexity here I haven't considered
         is LiteralCaseMatchExpression -> TypedInstance.from(type,matchExpression.value,factory.schema)
         else -> TODO()
      }
   }

   private fun evaluateSelector(selectorExpression: WhenSelectorExpression): TypedInstance {
      return when (selectorExpression) {
         is AccessorExpressionSelector -> factory.readAccessor(selectorExpression.declaredType.asVyneTypeReference(), selectorExpression.accessor)
         else -> TODO()
      }

   }

}
