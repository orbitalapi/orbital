package io.vyne.dataQuality

import com.winterbe.expekt.should
import io.vyne.models.Provided
import io.vyne.models.TypedInstance
import io.vyne.schemaStore.SimpleTaxiSchemaProvider
import io.vyne.utils.Benchmark
import org.junit.Test

class DataQualityRuleProviderTest {

   @Test
   fun `finds rules for type`() {
      val (schemaProvider, schema) = SimpleTaxiSchemaProvider.from(
         """
            ${DefaultRules.schema}

            type FirstName inherits String
            type PersonId inherits String
            model Person {
               id : PersonId inherits String
               name : FirstName inherits String
               dateOfBirth : DateOfBirth inherits Date
            }
            """
      )
      val ruleRegistry = RuleRegistry(
         applicabilityPredicates = DefaultRules.applicabilityPredicates,
         ruleEvaluators = DefaultRules.evaluators
      )
      val ruleProvider = DataQualityRuleProvider(schemaProvider, ruleRegistry)

      val typedInstance =
         TypedInstance.from(
            schema.type("Person"),
            """{ "id" : "007", "name" : null, "dateOfBirth" : "34-May-2002" }""",
            schema,
            source = Provided
         )

      Benchmark.benchmark("Evaluate quality rules") {
         ruleProvider.evaluate(typedInstance)
      }

      // Now assert
      val evaluationResult = ruleProvider.evaluate(typedInstance)

      evaluationResult.score.should.equal(25)
      evaluationResult.grade.should.equal(RuleGrade.BAD)
   }

   @Test
   fun gradeTableReturnsCorrectGrade() {
      val gradeTable = GradeTable(
         listOf(
            0..65 to RuleGrade.BAD,
            66..85 to RuleGrade.WARNING,
            86..100 to RuleGrade.GOOD
         )
      )
      gradeTable.grade(10).should.equal(RuleGrade.BAD)
      gradeTable.grade(65).should.equal(RuleGrade.BAD)
      gradeTable.grade(66).should.equal(RuleGrade.WARNING)
      gradeTable.grade(66).should.equal(RuleGrade.WARNING)
   }


}

