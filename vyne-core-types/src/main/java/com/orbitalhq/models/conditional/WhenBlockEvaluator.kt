package com.orbitalhq.models.conditional

import com.orbitalhq.models.AccessorReader
import com.orbitalhq.models.DataSource
import com.orbitalhq.models.EvaluationValueSupplier
import com.orbitalhq.models.FailedEvaluation
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.models.TypedNull
import com.orbitalhq.models.UndefinedSource
import com.orbitalhq.schemas.Schema
import com.orbitalhq.schemas.Type
import com.orbitalhq.schemas.toVyneQualifiedName
import lang.taxi.expressions.Expression
import lang.taxi.types.*
import java.util.*

class WhenBlockEvaluator(
   private val factory: EvaluationValueSupplier,
   private val schema: Schema,
   private val accessorReader: AccessorReader
) {
   suspend fun evaluate(
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
      val (caseBlock, whenCaseBlockSelection) = selectCaseBlock(selectorValue, readCondition, value, format)
      if (caseBlock == null) {
         // TODO : Update this to a WhenCaseEvalution data source
         return TypedNull.create(targetType, FailedEvaluation("No matching cases found in when clause"))
      }
      val result = accessorReader.read(
         value,
         targetType,
         caseBlock.getSingleAssignment().assignment,
         schema,
         source = whenCaseBlockSelection,
         allowContextQuerying = true,
         format = format
      )
      return result
   }

   private suspend fun selectCaseBlock(
      selectorValue: TypedInstance,
      readCondition: WhenExpression,
      value: Any,
      format: FormatsAndZoneOffset?
   ): Pair<WhenCaseBlock?, EvaluatedWhenCaseSelection> {
      var index = 0
      val evaluations = mutableListOf<TypedInstance>()
      val selectedCase = readCondition.cases.firstOrNull { caseBlock ->
         index = ++index
         if (caseBlock.matchExpression is ElseMatchExpression) {
            true
         } else {
            // see VyneQueryTest - `failures in boolean expression evalution should not terminate when condition evalutaions`()
            // without below 'catch' logic above test fails with 'com.orbitalhq.query.UnresolvedTypeInQueryException: No strategy found for discovering type Theme'
            val valueToCompare = try {
               evaluateExpression(caseBlock.matchExpression, selectorValue.type, value, format)
            } catch (e: Exception) {
               throw e
               // MP 10-Apr-24: Why were we treating all exceptions as false?
               // This was masking a scope error ("java.lang.IllegalStateException: Failed to resolve scope argument xx")
               // Once we understand the scneario here, let's make the catch more specific.
//               if (selectorValue.type.taxiType.basePrimitive == PrimitiveType.BOOLEAN) {
//                  TypedInstance.from(type = selectorValue.type, value = false, schema = schema)
//               } else {
//                  TypedNull.create(selectorValue.type)
//               }
            }
            evaluations.add(valueToCompare)
            selectorValue.valueEquals(valueToCompare)
         }


      }
      return selectedCase to EvaluatedWhenCaseSelection(readCondition.asTaxi(), selectorValue, selectedCase?.asTaxi(), evaluations)
   }

   private suspend fun evaluateExpression(
      matchExpression: Expression,
      type: Type,
      value: Any,
      format: FormatsAndZoneOffset?
   ): TypedInstance {
      // Which type should it be here?  Expression.returnType or type?
      return accessorReader.evaluate(value, type, matchExpression, dataSource = UndefinedSource, format = format)
   }

   private suspend fun evaluateSelector(
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

data class EvaluatedWhenCaseSelection(
   val expressionTaxi: String,
   val valueToMatch: TypedInstance,
   val matchedExpressionTaxi: String?,
   /**
    * All the case blocks that were evaluated.
    * The final entry will be the value that matched.
    * Not all case blocks are evaluated, only those up until a match is made
    */
   val evaluatedCases: List<TypedInstance>,
   override val id: String = UUID.randomUUID().toString()
) : DataSource {
   override val name: String = "Select case"
   override val failedAttempts: List<DataSource> = emptyList()

}
