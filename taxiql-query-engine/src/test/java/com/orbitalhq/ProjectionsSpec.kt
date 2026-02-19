package com.orbitalhq

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

// This is by no means the only place this is tested
// but I couldn't another clean file to write a test, and I'm a bit tired.
class ProjectionsSpec :  DescribeSpec({
   describe("queries with projections") {

      it("can return a field of a no-arg service in a projection") {
         val (vyne,stub) = testVyne("""
            closed model Person {
               id : PersonId inherits String
               name : Name inherits String
            }
            service MyApi {
               operation getPerson():Person
            }
         """.trimIndent())
         stub.addResponse("getPerson", """{ "id" : "123", "name" : "Jimmy" }""")

         // The issue here is that we're not searching for Person (which can be returned from the Direct strategy),
         // but Graph searcher can't invoke as there's no edges leading to the service.
         val result = vyne.query("""given { PersonId = "123" } find {
            | name : Name // <--- this is the test
            |}
         """.trimMargin())
            .firstRawObject()
         result.shouldBe(mapOf("name" to "Jimmy"))
      }

   }
}) {
}
