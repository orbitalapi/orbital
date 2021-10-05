package io.vyne.models.conditional

import io.vyne.models.AccessorReader
import io.vyne.models.DataSource
import io.vyne.models.EvaluationValueSupplier
import io.vyne.models.FailedEvaluation
import io.vyne.models.TypedInstance
import io.vyne.models.TypedNull
import io.vyne.models.safeTaxi
import io.vyne.schemas.AttributeName
import io.vyne.schemas.Schema
import io.vyne.schemas.Type
import io.vyne.utils.log
import lang.taxi.types.FieldSetExpression
import lang.taxi.types.WhenFieldSetCondition

class ConditionalFieldSetEvaluator(private val factory: EvaluationValueSupplier, private val schema:Schema, private val accessorReader: AccessorReader) {
   private val whenEvaluator = WhenFieldSetConditionEvaluator(factory, schema, accessorReader)
//   private val calculatedFieldEvaluator = CalculatedFieldSetExpressionEvaluator(factory, schema)

//   fun evaluate(readCondition: FieldSetExpression, targetType: Type): TypedInstance {
//      return evaluate(readCondition, attributeName = null, targetType = targetType)
//   }
   fun evaluate(value:Any,readCondition: FieldSetExpression, attributeName: AttributeName?, targetType: Type, datasource:DataSource): TypedInstance {
      return try {
         when (readCondition) {
            is WhenFieldSetCondition -> whenEvaluator.evaluate(value, readCondition,datasource, attributeName, targetType)
//            is CalculatedFieldSetExpression -> calculatedFieldEvaluator.evaluate(readCondition, attributeName, targetType)
//         is UnaryCalculatedFieldSetExpression -> calculatedFieldEvaluator.evaluate(readCondition, attributeName, targetType)
//         is TerenaryFieldSetExpression -> calculatedFieldEvaluator.evaluate(readCondition, attributeName, targetType)
            else -> error("Unhandled type of readCondition: ${readCondition::class.simpleName}")
         }
      } catch (e:Exception) {
         val exceptionMessage = e.message ?: "A ${e::class.simpleName} exception was thrown"
         val message = "Failed to evaluation expression ${readCondition.safeTaxi()}.  Will return null, and continue processing.  Details are captured in the lineage for this value - $exceptionMessage"
         log().warn(message, e)
         TypedNull.create(targetType, FailedEvaluation(message))
      }
   }
}
