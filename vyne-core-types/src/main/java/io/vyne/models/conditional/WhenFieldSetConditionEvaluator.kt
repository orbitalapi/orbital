package io.vyne.models.conditional

import io.vyne.models.*
import io.vyne.schemas.AttributeName
import io.vyne.schemas.Type
import io.vyne.schemas.asVyneTypeReference
import io.vyne.schemas.toVyneQualifiedName
import io.vyne.utils.log
import lang.taxi.types.*

class WhenFieldSetConditionEvaluator(private val factory: TypedObjectFactory) {
   private val valueExpressionEvaluator = ValueExpressionEvaluator(factory)
   fun evaluate(readCondition: WhenFieldSetCondition, attributeName: AttributeName?, targetType: Type): TypedInstance {
      val selectorValue = evaluateSelector(readCondition.selectorExpression)
      val caseBlock = selectCaseBlock(selectorValue, readCondition)
      val assignmentExpression = if (attributeName != null) {
         caseBlock.getAssignmentFor(attributeName)
      } else {
         caseBlock.getSingleAssignment()
      }
      val typedValue = evaluateExpression(assignmentExpression.assignment, targetType)
      return typedValue
   }

   private fun evaluateExpression(assignment: ValueAssignment, type: Type): TypedInstance {
//      val assignment = matchExpression.assignment
      return when (assignment) {
         is ScalarAccessorValueAssignment -> factory.readAccessor(type, assignment.accessor) // WTF? Why isn't the compiler working this out?
         is ReferenceAssignment -> factory.getValue(assignment.reference)
         is LiteralAssignment -> TypedInstance.from(type, assignment.value, factory.schema, true, source = DefinedInSchema)
         is DestructuredAssignment -> {
            val resolvedAttributes = assignment.assignments.map { nestedAssignment ->
               val attributeType = factory.schema.type(type.attribute(nestedAssignment.fieldName).type)
               nestedAssignment.fieldName to evaluateExpression(nestedAssignment.assignment, attributeType)
            }.toMap()
            TypedObject.fromAttributes(type, resolvedAttributes, factory.schema, true, source = MixedSources)
         }
         is EnumValueAssignment -> {
            val enumType = factory.schema.type(assignment.enum.qualifiedName)
            // TODO : SHouldn't the enumValue be the actual TypedInstance?
            // TODO : Probably could use a better data source here.
            TypedInstance.from(enumType, assignment.enumValue.value, factory.schema, source = DefinedInSchema)
         }
         is NullAssignment -> {
            TypedNull(type, source = DefinedInSchema)
         }
         else -> {
            log().warn("Unexpected assignment $assignment")
            TODO()
         }
      }
   }

   private fun selectCaseBlock(selectorValue: TypedInstance, readCondition: WhenFieldSetCondition): WhenCaseBlock {
      return readCondition.cases.firstOrNull { caseBlock ->
         when (caseBlock.matchExpression) {
            is ElseMatchExpression -> true
            is ValueExpression -> {
               valueExpressionEvaluator.expressionEvaluatesEqualTo(caseBlock.matchExpression as ValueExpression, selectorValue.type, selectorValue)
            }
            else -> {
               log().error("Unhandled caseBlock expression - ${caseBlock.matchExpression::class.simpleName}")
               false
            }
         }


      } ?: error("No matching cases found")

   }


   private fun evaluateSelector(selectorExpression: WhenSelectorExpression): TypedInstance {
      return when (selectorExpression) {
         is AccessorExpressionSelector -> {
            // Note: I had to split this across several lines
            // as the compiler was getting confused and throwing
            // method not found exceptions.
            // Probably just a local issue, can refactor later
            val typeReference = selectorExpression.declaredType.toQualifiedName().toVyneQualifiedName()
            val instance = factory.readAccessor(typeReference, selectorExpression.accessor, nullable = false)
            instance
         }
         is FieldReferenceSelector -> {
            factory.getValue(selectorExpression.fieldName)
         }
         else -> TODO("WhenFieldSetConditionEvaluator.evaluateSelector not handled for type ${selectorExpression::class.simpleName}")
      }

   }

}
