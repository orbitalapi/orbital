package io.vyne.models.conditional

import io.vyne.models.DefinedInSchema
import io.vyne.models.EvaluationValueSupplier
import io.vyne.models.MixedSources
import io.vyne.models.TypedInstance
import io.vyne.models.TypedNull
import io.vyne.models.TypedObject
import io.vyne.schemas.AttributeName
import io.vyne.schemas.Schema
import io.vyne.schemas.Type
import io.vyne.schemas.toVyneQualifiedName
import io.vyne.utils.log
import lang.taxi.types.AccessorExpressionSelector
import lang.taxi.types.AssignmentExpression
import lang.taxi.types.DestructuredAssignment
import lang.taxi.types.ElseMatchExpression
import lang.taxi.types.EmptyReferenceSelector
import lang.taxi.types.EnumValueAssignment
import lang.taxi.types.FieldReferenceSelector
import lang.taxi.types.LiteralAssignment
import lang.taxi.types.LiteralCaseMatchExpression
import lang.taxi.types.NullAssignment
import lang.taxi.types.ReferenceAssignment
import lang.taxi.types.ReferenceCaseMatchExpression
import lang.taxi.types.ScalarAccessorValueAssignment
import lang.taxi.types.ValueAssignment
import lang.taxi.types.WhenCaseBlock
import lang.taxi.types.WhenCaseMatchExpression
import lang.taxi.types.WhenFieldSetCondition
import lang.taxi.types.WhenSelectorExpression

class WhenFieldSetConditionEvaluator(private val factory: EvaluationValueSupplier, private val schema:Schema) {
   fun evaluate(readCondition: WhenFieldSetCondition, attributeName: AttributeName?, targetType: Type): TypedInstance {
      return when (readCondition.selectorExpression) {
         is EmptyReferenceSelector -> {
            val assignmentExpression = evaluateLogicalWhenCases(readCondition, attributeName, targetType)
            evaluateExpression(assignmentExpression.assignment, targetType)
         }
         else -> {
            val selectorValue = evaluateSelector(readCondition.selectorExpression)
            val caseBlock = selectCaseBlock(selectorValue, readCondition)
            val assignmentExpression = if (attributeName != null) {
               caseBlock.getAssignmentFor(attributeName)
            } else {
               caseBlock.getSingleAssignment()
            }
            val typedValue = evaluateExpression(assignmentExpression.assignment, targetType)
            typedValue
         }
      }
   }

   private fun evaluateLogicalWhenCases(readCondition: WhenFieldSetCondition, attributeName: AttributeName?, targetType: Type): AssignmentExpression {
      val retVal =  LogicalExpressionEvaluator.evaluate(readCondition.cases, factory, targetType)?.let {
         if (attributeName != null) {
            it.getAssignmentFor(attributeName)
         } else {
            it.getSingleAssignment()
         }
      }
      return retVal ?: error("unexpected result when evaluating logical expressions for a when. ensure that your when clause has 'else' part.")
   }

   private fun evaluateExpression(assignment: ValueAssignment, type: Type): TypedInstance {
      return when (assignment) {
         is ScalarAccessorValueAssignment -> factory.readAccessor(type, assignment.accessor) // WTF? Why isn't the compiler working this out?
         is ReferenceAssignment -> factory.getValue(assignment.reference)
         is LiteralAssignment -> TypedInstance.from(type, assignment.value, schema, true, source = DefinedInSchema)
         is DestructuredAssignment -> {
            val resolvedAttributes = assignment.assignments.map { nestedAssignment ->
               val attributeType = schema.type(type.attribute(nestedAssignment.fieldName).type)
               nestedAssignment.fieldName to evaluateExpression(nestedAssignment.assignment, attributeType)
            }.toMap()
            TypedObject.fromAttributes(type, resolvedAttributes, schema, true, source = MixedSources)
         }
         is EnumValueAssignment -> {
            val enumType = schema.type(assignment.enum.qualifiedName)
            // TODO : SHouldn't the enumValue be the actual TypedInstance?
            // TODO : Probably could use a better data source here.
            TypedInstance.from(enumType, assignment.enumValue.value, schema, source = DefinedInSchema)
         }
         is NullAssignment -> {
            TypedNull.create(type, source = DefinedInSchema)
         }
         else -> {
            log().warn("Unexpected assignment $assignment")
            TODO()
         }
      }
   }

   private fun selectCaseBlock(selectorValue: TypedInstance, readCondition: WhenFieldSetCondition): WhenCaseBlock {
      return readCondition.cases.firstOrNull { caseBlock ->
         if (caseBlock.matchExpression is ElseMatchExpression) {
            true
         } else {
            val valueToCompare = evaluateExpression(caseBlock.matchExpression, selectorValue.type)
            selectorValue.valueEquals(valueToCompare)
         }


      } ?: error("No matching cases found in when clause")

   }

   private fun evaluateExpression(matchExpression: WhenCaseMatchExpression, type: Type): TypedInstance {
      return when (matchExpression) {
         is ReferenceCaseMatchExpression -> factory.getValue(matchExpression.reference)
         // Note - I'm assuming the literal value is the same type as what we're comparing to.
         // Reasonable for now, but suspect subtypes etc may cause complexity here I haven't considered
         is LiteralCaseMatchExpression -> TypedInstance.from(type, matchExpression.value, schema, source = DefinedInSchema)
         //is LogicalExpression ->
         else -> {
            log().warn("Unexpected match Expression $matchExpression")
            TODO()
         }
      }
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
