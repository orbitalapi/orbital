package com.orbitalhq

import com.orbitalhq.query.VyneQlGrammar
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class StubServiceSpec : DescribeSpec({
  describe("stub service") {
     it("can stub table operations") {
        val (vyne,stub) = testVyne("""

          namespace com.foo.test

          model Person {
             id : PersonId inherits String
          }

          service PersonDb {
            table people : Person[]
          }
        """.trimIndent(), VyneQlGrammar.QUERY_TYPE_TAXI)
        stub.addTableFindOneResponse("people", """{ "id" : "123" }""")
        vyne.query("""
           find { Person(PersonId == "123") }
        """.trimIndent())
           .firstRawObject()

        stub.addTableFindManyResponse("people", """[{ "id" : "123" }]""")
        vyne.query("""find { Person[] }""")
           .rawObjects()
     }

     it("can stub operations by their full name") {
         val (vyne,stub) = testVyne("""
            model Person {
               name : String
            }

            service PersonApi {
               operation getPerson():Person
            }
         """.trimIndent())
        stub.addResponse("PersonApi@@getPerson", """{ "name" : "Jimmy" }""")
        vyne.query("find { Person }")
           .firstRawObject()
           .shouldBe(mapOf("name" to "Jimmy"))
     }
  }
})
