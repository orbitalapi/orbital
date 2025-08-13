package com.orbitalhq

import com.orbitalhq.query.VyneQlGrammar
import io.kotest.core.spec.style.DescribeSpec

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
  }
})
