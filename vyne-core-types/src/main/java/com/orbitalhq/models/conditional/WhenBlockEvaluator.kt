package com.orbitalhq.models.conditional

import com.orbitalhq.models.AccessorReader
import com.orbitalhq.models.DataSource
import com.orbitalhq.models.EvaluationValueSupplier
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.models.TypedNull
import com.orbitalhq.models.UndefinedSource
import com.orbitalhq.schemas.Schema
import com.orbitalhq.schemas.Type
import com.orbitalhq.schemas.toVyneQualifiedName
import lang.taxi.expressions.Expression
import lang.taxi.types.*

class WhenBlockEvaluator(
   private val factory: EvaluationValueSupplier,
   private val schema: Schema,
   private val accessorReader: AccessorReader
) {
   fun evaluate(
      value: Any,
      readCondition: WhenExpression,
      source: DataSource,
      targetType: Type,
      format: FormatsAndZoneOffset?
   ): TypedInstance {
      val selectorExpression = readCondition.selectorExpression
      val selectorValue = accessorReader.evaluate(
         value,
         schema.type(selectorExpression.returnType),
         selectorExpression,
         dataSource = source,
         format = format
      )
      val caseBlock = selectCaseBlock(selectorValue, readCondition, value, format)

      return accessorReader.read(
         value,
         targetType,
         caseBlock.getSingleAssignment().assignment,
         schema,
         source = source,
         allowContextQuerying = true,
         format = format
      )
   }

   private fun selectCaseBlock(
      selectorValue: TypedInstance,
      readCondition: WhenExpression,
      value: Any,
      format: FormatsAndZoneOffset?
   ): WhenCaseBlock {
      var index = 0
      return readCondition.cases.firstOrNull { caseBlock ->
         index = ++index
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

   private fun evaluateExpression(
      matchExpression: Expression,
      type: Type,
      value: Any,
      format: FormatsAndZoneOffset?
   ): TypedInstance {
      // Which type should it be here?  Expression.returnType or type?
      return accessorReader.evaluate(value, type, matchExpression, dataSource = UndefinedSource, format = format)
   }

   private fun evaluateSelector(
      selectorExpression: WhenSelectorExpression,
      format: FormatsAndZoneOffset?
   ): TypedInstance {
      return when (selectorExpression) {
         is AccessorExpressionSelector -> {
            // Note: I had to split this across several lines
            // as the compiler was getting confused and throwing
            // method not found exceptions.
            // Probably just a local issue, can refactor later
            val typeReference = selectorExpression.declaredType.toQualifiedName().toVyneQualifiedName()
            val instance =
               factory.readAccessor(typeReference, selectorExpression.accessor, nullable = false, format = format)
            instance
         }

         is FieldReferenceSelector -> {
            factory.getValue(selectorExpression.fieldName)
         }

         else -> TODO("WhenFieldSetConditionEvaluator.evaluateSelector not handled for type ${selectorExpression::class.simpleName}")
      }

   }

}
