package com.orbitalhq.models.conditional

import com.orbitalhq.models.AccessorReader
import com.orbitalhq.models.DataSource
import com.orbitalhq.models.EvaluationValueSupplier
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.models.TypedNull
import com.orbitalhq.models.UndefinedSource
import com.orbitalhq.schemas.AttributeName
import com.orbitalhq.schemas.Schema
import com.orbitalhq.schemas.Type
import com.orbitalhq.schemas.toVyneQualifiedName
import lang.taxi.expressions.Expression
import lang.taxi.types.*

class WhenFieldSetConditionEvaluator(private val factory: EvaluationValueSupplier, private val schema:Schema, private val accessorReader: AccessorReader) {
   fun evaluate(value:Any,readCondition: WhenFieldSetCondition, source:DataSource, attributeName: AttributeName?, targetType: Type, format: FormatsAndZoneOffset?): TypedInstance {
      val selectorExpression = readCondition.selectorExpression
      val selectorValue = accessorReader.evaluate(value,schema.type(selectorExpression.returnType),selectorExpression,dataSource = source, format =format)
      val caseBlock = selectCaseBlock(selectorValue, readCondition, value, format)

      /**
       * Enrichment logic updated so that we no longer try to evaluate the target type. Instead we try to resolve case conditions one by one.
       * Therefore, allowContextQuerying set to true here.
       */
      val evaluatedAssignment = accessorReader.read(value, targetType, caseBlock.getSingleAssignment().assignment, schema, source = source, allowContextQuerying = true, format = format)
      return evaluatedAssignment
//      return when (readCondition.selectorExpression) {
//         is EmptyReferenceSelector -> {
//            val assignmentExpression = evaluateLogicalWhenCases(readCondition, attributeName, targetType)
//            evaluateExpression(assignmentExpression.assignment, targetType)
//         }
//         else -> {
//            val selectorValue = evaluateSelector(readCondition.selectorExpression)
//            val caseBlock = selectCaseBlock(selectorValue, readCondition)
//            val assignmentExpression = if (attributeName != null) {
//               caseBlock.getAssignmentFor(attributeName)
//            } else {
//               caseBlock.getSingleAssignment()
//            }
//            val typedValue = evaluateExpression(assignmentExpression.assignment, targetType)
//            typedValue
//         }
//      }
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

   private fun evaluateExpression(assignment: AssignmentExpression, type: Type): TypedInstance {
      TODO()
//      return when (assignment) {
//         is ScalarAccessorValueAssignment -> factory.readAccessor(type, assignment.accessor) // WTF? Why isn't the compiler working this out?
//         is ReferenceAssignment -> factory.getValue(assignment.reference)
//         is LiteralAssignment -> TypedInstance.from(type, assignment.value, schema, true, source = DefinedInSchema)
//         is DestructuredAssignment -> {
//            val resolvedAttributes = assignment.assignments.map { nestedAssignment ->
//               val attributeType = schema.type(type.attribute(nestedAssignment.fieldName).type)
//               nestedAssignment.fieldName to evaluateExpression(nestedAssignment.assignment, attributeType)
//            }.toMap()
//            TypedObject.fromAttributes(type, resolvedAttributes, schema, true, source = MixedSources)
//         }
//         is EnumValueAssignment -> {
//            val enumType = schema.type(assignment.enum.qualifiedName)
//            // TODO : SHouldn't the enumValue be the actual TypedInstance?
//            // TODO : Probably could use a better data source here.
//            TypedInstance.from(enumType, assignment.enumValue.value, schema, source = DefinedInSchema)
//         }
//         is NullAssignment -> {
//            TypedNull.create(type, source = DefinedInSchema)
//         }
//         else -> {
//            log().warn("Unexpected assignment $assignment")
//            TODO()
//         }
//      }
   }

   private fun selectCaseBlock(selectorValue: TypedInstance, readCondition: WhenFieldSetCondition, value: Any, format: FormatsAndZoneOffset?): WhenCaseBlock {
      var index = 0
      return readCondition.cases.firstOrNull { caseBlock ->
         index =  ++index
         if (caseBlock.matchExpression is ElseMatchExpression) {
            true
         } else {
            // see VyneQueryTest - `failures in boolean expression evalution should not terminate when condition evalutaions`()
            // without below 'catch' logic above test fails with 'com.orbitalhq.query.UnresolvedTypeInQueryException: No strategy found for discovering type Theme'
            val valueToCompare = try {
               evaluateExpression(caseBlock.matchExpression, selectorValue.type, value, format)
            } catch (e: Exception) {
               if (selectorValue.type.taxiType.basePrimitive == PrimitiveType.BOOLEAN) {
                  TypedInstance.from(type = selectorValue.type, value = false, schema = schema)
               } else {
                  TypedNull.create(selectorValue.type)
               }
            }
            selectorValue.valueEquals(valueToCompare)
         }


      } ?: error("No matching cases found in when clause")

   }

   private fun evaluateExpression(matchExpression: Expression, type: Type, value:Any, format: FormatsAndZoneOffset?): TypedInstance {
      // Which type should it be here?  Expression.returnType or type?
      val evaluated = accessorReader.evaluate(value,type, matchExpression, dataSource = UndefinedSource, format = format)
      return evaluated
//      return when (matchExpression) {
//         is ReferenceCaseMatchExpression -> factory.getValue(matchExpression.reference)
//         // Note - I'm assuming the literal value is the same type as what we're comparing to.
//         // Reasonable for now, but suspect subtypes etc may cause complexity here I haven't considered
//         is LiteralCaseMatchExpression -> TypedInstance.from(type, matchExpression.value, schema, source = DefinedInSchema)
//         //is LogicalExpression ->
//         else -> {
//            log().warn("Unexpected match Expression $matchExpression")
//            TODO()
//         }
//      }
   }

   private fun evaluateSelector(selectorExpression: WhenSelectorExpression, format: FormatsAndZoneOffset?): TypedInstance {
      return when (selectorExpression) {
         is AccessorExpressionSelector -> {
            // Note: I had to split this across several lines
            // as the compiler was getting confused and throwing
            // method not found exceptions.
            // Probably just a local issue, can refactor later
            val typeReference = selectorExpression.declaredType.toQualifiedName().toVyneQualifiedName()
            val instance = factory.readAccessor(typeReference, selectorExpression.accessor, nullable = false, format = format)
            instance
         }
         is FieldReferenceSelector -> {
            factory.getValue(selectorExpression.fieldName)
         }
         else -> TODO("WhenFieldSetConditionEvaluator.evaluateSelector not handled for type ${selectorExpression::class.simpleName}")
      }

   }

}