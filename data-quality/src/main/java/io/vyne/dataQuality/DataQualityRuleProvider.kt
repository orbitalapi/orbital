package io.vyne.dataQuality

import io.vyne.models.*
import io.vyne.schemaStore.SchemaProvider
import io.vyne.schemas.Field
import lang.taxi.dataQuality.DataQualityRule
import lang.taxi.types.AttributePath
import lang.taxi.types.PrimitiveType
import org.springframework.stereotype.Component

@Component
class DataQualityRuleProvider(
   private val schemaProvider: SchemaProvider,
   private val ruleRegistry: RuleRegistry
) {

   private fun getRulesFor(typedInstance: TypedInstance, field: Field?): List<DataQualityRule> {
      return schemaProvider.schema().taxi.dataQualityRules
         .filter { rule ->
            ruleAppliesTo(rule, typedInstance, field)
         }
   }

   private fun ruleAppliesTo(rule: DataQualityRule, typedInstance: TypedInstance, field: Field?): Boolean {
      val schema = schemaProvider.schema()
      val booleanType = schema.type(PrimitiveType.BOOLEAN.qualifiedName)
      return rule.applyToFunctions
         .stream()
         .allMatch { function ->
            val predicate = ruleRegistry.getApplicabilityPredicate(function.toQualifiedName())
            predicate.applies(typedInstance.type, field)
         }
   }

   fun evaluate(typedInstance: TypedInstance, gradeTable: GradeTable = GradeTable.DEFAULT): AveragedDataQualityEvaluation {
      val evaluations =  doEvaluate(typedInstance, null, AttributePath.EMPTY)
         .filter { it.result.grade != RuleGrade.NOT_APPLICABLE }
      return AveragedDataQualityEvaluation(evaluations, gradeTable)
   }

   private fun doEvaluate(
      typedInstance: TypedInstance,
      field: Field?,
      attributePath: AttributePath
   ): List<AttributeDataQualityRuleEvaluation> {
      return when (typedInstance) {
         is TypedObject -> {
            // Evaluate the fields first
            typedInstance.type.attributes.flatMap { (fieldName, field) ->
               val attribute = typedInstance[fieldName]
               doEvaluate(attribute, field, attributePath.append(fieldName))
            }
         }
         is TypedValue, is TypedNull, is TypedError -> {
            getRulesFor(typedInstance, field)
               .map { rule -> ruleRegistry.evaluate(rule, typedInstance, attributePath) }
         }
         else -> {
            TODO("No evaluation logic for type of ${typedInstance::class.simpleName}")
         }
      }

   }
}

