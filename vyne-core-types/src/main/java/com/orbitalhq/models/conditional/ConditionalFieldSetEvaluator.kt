package com.orbitalhq.models.conditional

import com.orbitalhq.models.AccessorReader
import com.orbitalhq.models.DataSource
import com.orbitalhq.models.EvaluationValueSupplier
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.schemas.AttributeName
import com.orbitalhq.schemas.Schema
import com.orbitalhq.schemas.Type
import lang.taxi.types.FieldSetExpression

class ConditionalFieldSetEvaluator(private val factory: EvaluationValueSupplier, private val schema:Schema, private val accessorReader: AccessorReader) {
   private val whenEvaluator = WhenBlockEvaluator(factory, schema, accessorReader)
//   private val calculatedFieldEvaluator = CalculatedFieldSetExpressionEvaluator(factory, schema)

//   fun evaluate(readCondition: FieldSetExpression, targetType: Type): TypedInstance {
//      return evaluate(readCondition, attributeName = null, targetType = targetType)
//   }
   fun evaluate(value:Any,readCondition: FieldSetExpression, attributeName: AttributeName?, targetType: Type, datasource:DataSource): TypedInstance {
      TODO("Refactor when blocks")
//      return try {
//         when (readCondition) {
//            is WhenExpression -> whenEvaluator.evaluate(value, readCondition,datasource, attributeName, targetType, null)
////            is CalculatedFieldSetExpression -> calculatedFieldEvaluator.evaluate(readCondition, attributeName, targetType)
////         is UnaryCalculatedFieldSetExpression -> calculatedFieldEvaluator.evaluate(readCondition, attributeName, targetType)
////         is TerenaryFieldSetExpression -> calculatedFieldEvaluator.evaluate(readCondition, attributeName, targetType)
//            else -> error("Unhandled type of readCondition: ${readCondition::class.simpleName}")
//         }
//      } catch (e:Exception) {
//         val exceptionMessage = e.message ?: "A ${e::class.simpleName} exception was thrown"
//         val message = "Failed to evaluation expression ${readCondition.safeTaxi()}.  Will return null, and continue processing.  Details are captured in the lineage for this value - $exceptionMessage"
//         log().warn(message, e)
//         TypedNull.create(targetType, FailedEvaluation(message))
//      }
   }
}
