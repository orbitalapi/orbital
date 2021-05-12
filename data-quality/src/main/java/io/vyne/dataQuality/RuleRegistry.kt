package io.vyne.dataQuality

import io.vyne.models.TypedInstance
import lang.taxi.dataQuality.DataQualityRule
import lang.taxi.types.AttributePath
import lang.taxi.types.QualifiedName

class RuleRegistry(
   private val applicabilityPredicates: List<RuleApplicabilityPredicate>,
   private val ruleEvaluators: List<DataQualityRuleEvaluator>
) {

   companion object {
      fun default(): RuleRegistry {
         return RuleRegistry(
            DefaultRules.applicabilityPredicates,
            DefaultRules.evaluators
         )
      }
   }

   private val evaluatorsByName = ruleEvaluators.associateBy { it.qualifiedName }
   private val predicatesByName = applicabilityPredicates.associateBy { it.functionName }
   fun getApplicabilityPredicate(taxiQualifiedName: QualifiedName): RuleApplicabilityPredicate {
      return predicatesByName[taxiQualifiedName]
         ?: error("No applicability rule has been registered with name $taxiQualifiedName")
   }

   fun evaluate(
      rule: DataQualityRule,
      instance: TypedInstance?,
      attributePath: AttributePath
   ): AttributeDataQualityRuleEvaluation {
      val evaluator = evaluatorsByName[rule.toQualifiedName()]
         ?: error("No rule evaluator has been registered for rule ${rule.toQualifiedName()}")
      return AttributeDataQualityRuleEvaluation(
         attributePath,
         evaluator.evaluate(instance)
      )
   }
}
