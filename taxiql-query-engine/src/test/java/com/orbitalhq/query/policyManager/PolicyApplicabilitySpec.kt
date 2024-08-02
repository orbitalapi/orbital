package com.orbitalhq.query.policyManager

import com.orbitalhq.schemas.PolicyWithPath
import com.orbitalhq.schemas.taxi.TaxiSchema
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSingleElement
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldBeEmpty

class PolicyApplicabilitySpec : DescribeSpec({
   describe("Tests around checking which policies apply to an object") {
      val baseSchema = """
         model Person {
            name : PersonName inherits String
         }
         model Actor inherits Person {
            salary : Salary inherits Int
            agent : Agent
         }
         model Agent inherits Person {
            commission : Commission inherits Decimal
         }
         model ProductionCompany {
            name : ProductionCompanyName inherits String
         }
         model Film {
            title : FilmTitle inherits String
            producer : ProductionCompany
            cast : Actor[]
         }
      """.trimIndent()

      fun schemaWithPolicy(policy: String): TaxiSchema {
         return TaxiSchema.from(
            """
            $baseSchema

            $policy
         """.trimIndent()
         )
      }

      fun TaxiSchema.policiesForType(typeName: String): List<PolicyWithPath> {
         return PolicyEvaluator().findPolicies(this, this.type(typeName))
      }
      it("matches a policy on the root type") {
         schemaWithPolicy(
            """
            policy TestPolicy against Film {}
         """
         ).policiesForType("Film")
            .shouldHaveSinglePolicy("TestPolicy")
            .path.shouldBeEmpty()
      }
      it("matches a policy defined against a base type of a root type") {
         val policy = schemaWithPolicy(
            """
            policy TestPolicy against Person {}
         """
         ).policiesForType("Agent")
            .shouldHaveSinglePolicy("TestPolicy")
         policy.policiedType.fullyQualifiedName.shouldBe("Person")
      }
      it("does not match a policy defined against a subtype of a root type") {
         schemaWithPolicy(
            """
            policy TestPolicy against Actor {}
         """
         ).policiesForType("Person")
            .shouldBeEmpty()
      }

      it("matches a policy defined against a nested field of a collection member") {
         val policy = schemaWithPolicy(
            """
            policy TestPolicy against Commission {}
         """
         ).policiesForType("Film")
            .shouldHaveSinglePolicy("TestPolicy")
         policy.policiedType.fullyQualifiedName.shouldBe("Commission")
         policy.path.shouldBe("cast.agent.commission")
      }

      it("matches a policy defined against a field" ) {
         val policy = schemaWithPolicy(
            """
            policy TestPolicy against ProductionCompany {}
         """
         ).policiesForType("Film")
            .shouldHaveSinglePolicy("TestPolicy")
         policy.policiedType.fullyQualifiedName.shouldBe("ProductionCompany")
         policy.path.shouldBe("producer")
      }

      it("matches a policy defined against a nested field" ) {
         val policy = schemaWithPolicy(
            """
            policy TestPolicy against ProductionCompanyName {}
         """
         ).policiesForType("Film")
            .shouldHaveSinglePolicy("TestPolicy")
         policy.policiedType.fullyQualifiedName.shouldBe("ProductionCompanyName")
         policy.path.shouldBe("producer.name")
      }
   }
})

private fun List<PolicyWithPath>.shouldHaveSinglePolicy(policyName: String): PolicyWithPath {
   return withClue("Should have policy $policyName") {
      this.shouldHaveSingleElement { it.policy.qualifiedName == policyName }
      this.single { it.policy.qualifiedName == policyName }
   }

}
