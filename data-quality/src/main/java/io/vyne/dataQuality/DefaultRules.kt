package io.vyne.dataQuality

import io.vyne.models.TypedError
import io.vyne.models.TypedInstance
import io.vyne.models.TypedNull
import io.vyne.schemas.Field
import io.vyne.schemas.Type
import lang.taxi.types.QualifiedName
import reactor.core.publisher.Mono

// Whilst spiking, these are the rules, schemas, etc
// that I'm using to prep for a demo.
// This whole thing should not be hard-coded, and we should be
// using schemas, schema modules, and loaders to pull this stuff in.
object DefaultRules {
   val schema = """
      declare function isNotNullable():Boolean
            declare function everything():Boolean
            type rule ShouldNotFailToParse {
               applyTo {
                  everything()
               }
            }
            type rule MandatoryShouldNotBeNull {
               applyTo {
                  isNotNullable()
               }
            }
   """.trimIndent()
   val evaluators = listOf(
      MandatoryShouldNotBeNull(),
      ShouldNotFailToParse()
   )
   val applicabilityPredicates = listOf(
      IsNullableRulePredicate(),
      IsNotNullableRulePredicate(),
      ApplyToEverythingRulePredicate()
   )
}

private class ApplyToEverythingRulePredicate : RuleApplicabilityPredicate {

   override val functionName: QualifiedName = QualifiedName.from("everything")
   override fun applies(type: Type, field: Field?): Boolean = true
}

private class IsNullableRulePredicate : RuleApplicabilityPredicate {

   override val functionName: QualifiedName = QualifiedName.from("isNullable")
   override fun applies(type: Type, field: Field?): Boolean {
      return field?.nullable ?: false
   }
}

private class IsNotNullableRulePredicate : RuleApplicabilityPredicate {
   override val functionName: QualifiedName = QualifiedName.from("isNotNullable")
   override fun applies(type: Type, field: Field?): Boolean {
      return if (field == null) {
         true
      } else {
         !field.nullable
      }
   }
}

private class ShouldNotFailToParse : DataQualityRuleEvaluator {
   companion object {
      val NAME = QualifiedName.from("ShouldNotFailToParse")
      val FAIL = DataQualityRuleEvaluation(NAME, 0, RuleGrade.BAD)
      val NOT_APPLICABLE = DataQualityRuleEvaluation(NAME, 0, RuleGrade.NOT_APPLICABLE)
   }

   override val qualifiedName: QualifiedName = NAME

   override fun evaluate(instance: TypedInstance?): Mono<DataQualityRuleEvaluation> {
      return Mono.just(
         when (instance) {
            is TypedError -> FAIL
            else -> NOT_APPLICABLE
         }
      )
   }
}

private class MandatoryShouldNotBeNull : DataQualityRuleEvaluator {
   companion object {
      val NAME = QualifiedName.from("MandatoryShouldNotBeNull")
      val FAIL = DataQualityRuleEvaluation(NAME, 0, RuleGrade.BAD)
      val PASS = DataQualityRuleEvaluation(NAME, 100, RuleGrade.GOOD)
   }

   override val qualifiedName: QualifiedName = NAME
   override fun evaluate(instance: TypedInstance?): Mono<DataQualityRuleEvaluation> {
      return Mono.just(
         when {
            instance == null -> FAIL
            instance.value == null -> FAIL
            instance is TypedNull -> FAIL
            instance.value == "" -> FAIL
            else -> PASS
         }
      )
   }

}
