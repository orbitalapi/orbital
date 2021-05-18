package io.vyne.dataQuality

import io.vyne.models.*
import io.vyne.schemaStore.SchemaProvider
import io.vyne.schemas.Field
import lang.taxi.dataQuality.DataQualityRule
import lang.taxi.types.AttributePath
import lang.taxi.types.PrimitiveType
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

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

   fun evaluate(
      typedInstance: TypedInstance,
      gradeTable: GradeTable = GradeTable.DEFAULT
   ): Mono<AveragedDataQualityEvaluation> {
      return doEvaluate(typedInstance, null, AttributePath.EMPTY)
         .filter { it.result.grade != RuleGrade.NOT_APPLICABLE }
         .collectList()
         .map { evaluations -> AveragedDataQualityEvaluation(evaluations, gradeTable) }
   }

   private fun doEvaluate(
      typedInstance: TypedInstance,
      field: Field?,
      attributePath: AttributePath
   ): Flux<AttributeDataQualityRuleEvaluation> {
      return when (typedInstance) {
         is TypedObject -> {
            // Evaluate the fields first
            Flux.merge(typedInstance.type.attributes.map { (fieldName, field) ->
               val attribute = typedInstance[fieldName]
               doEvaluate(attribute, field, attributePath.append(fieldName))
            })
         }
         is TypedValue, is TypedNull, is TypedError -> {
            Flux.merge(getRulesFor(typedInstance, field)
               .map { rule -> ruleRegistry.evaluate(rule, typedInstance, attributePath) }
            )
         }
         else -> {
            TODO("No evaluation logic for type of ${typedInstance::class.simpleName}")
         }
      }

   }
}

