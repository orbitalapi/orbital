package com.orbitalhq

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

class OperationsAsExpressionsSpec : DescribeSpec ({
   it("should let me call an operation as an expression") {
      val (vyne,stub) = testVyne("""
model Person {
   name : PersonName inherits String
}
service PersonService {
   operation getPerson(emailAddress: String):Person
}
      """.trimIndent())
      stub.addResponse("getPerson", """{ "name" : "Jimmy" }""")
      val result = vyne.query("""
         find {
            person : Person = PersonService::getPerson("marty")
         }
      """.trimIndent())
         .firstRawObject()
      result.shouldBe(mapOf("person" to mapOf("name" to "Jimmy") ))
   }

   it("should let me call an operation as an expression by passing a value from a given clause") {
      val (vyne,stub) = testVyne("""
model Person {
   name : PersonName inherits String
}
service PersonService {
   operation getPerson(emailAddress: String):Person
}
      """.trimIndent())
      stub.addResponse("getPerson", """{ "name" : "Jimmy" }""")
      val result = vyne.query("""
         given { name:String = "marty" }
         find {
            person : Person = PersonService::getPerson(name)
         }
      """.trimIndent())
         .firstRawObject()
      stub.lastCall("getPerson").single().toRawObject().shouldBe("marty")
      result.shouldBe(mapOf("person" to mapOf("name" to "Jimmy") ))
   }

   it("should let me call an operation as an expression by passing a value from an iterated value") {
      val (vyne,stub) = testVyne("""
model Person {
   name : PersonName inherits String
}
service PersonService {
   operation getPerson(emailAddress: String):Person
}
      """.trimIndent())
      stub.addResponse("getPerson", """{ "name" : "Jimmy" }""")
      val result = vyne.query("""
         given { names:String[] = ["jim", "jack"] }
         find { names } as (name:String) -> {
            person : Person = PersonService::getPerson(name)
         }[]
      """.trimIndent())
         .rawObjects()
      result.shouldHaveSize(2)
      // Verified we called twice
      val calls = stub.calls["getPerson"]
      calls.shouldHaveSize(2)

      // verify the correct args were passed
      // Actual order that things were called is not guaranteed
      calls.map { it.single().toRawObject() }
         .shouldContainAll("jim", "jack")
      result.shouldBe(
         listOf(
         mapOf("person" to mapOf("name" to "Jimmy") ), // note: both are Jimmy, as the stub returns the same value
         mapOf("person" to mapOf("name" to "Jimmy") ),
         )
      )
   }


   it("should let me call a no-args operation as an expression") {
      val (vyne,stub) = testVyne("""
model Person {
   name : PersonName inherits String
}
service PersonService {
   operation getPerson():Person
}
      """.trimIndent())
      stub.addResponse("getPerson", """{ "name" : "Jimmy" }""")
      val result = vyne.query("""
         find {
            person : Person = PersonService::getPerson()
         }
      """.trimIndent())
         .firstRawObject()
      result.shouldBe(mapOf("person" to mapOf("name" to "Jimmy") ))
   }

   it("should let return null if the operation returns null") {
      val (vyne,stub) = testVyne("""
model Person {
   name : PersonName inherits String
}
service PersonService {
   operation getPerson():Person
}
      """.trimIndent())
      stub.addResponse("getPerson", "")
      val result = vyne.query("""
         find {
            person : Person = PersonService::getPerson()
         }
      """.trimIndent())
         .firstRawObject()
      result.shouldBe(mapOf("person" to null))
   }


   it("should let me call an operation returning a collection as an expression") {
      val (vyne,stub) = testVyne("""
model Person {
   name : PersonName inherits String
}
service PersonService {
   operation getAllPeople(emailAddress: String):Person[]
}
      """.trimIndent())
      stub.addResponse("getAllPeople", """[{ "name" : "Jimmy" }, { "name" : "Bob" }]""")
      val result = vyne.query("""
         find {
            person : Person[] = PersonService::getAllPeople("marty")
         }
      """.trimIndent())
         .firstRawObject()
      result.shouldBe(mapOf("person" to listOf(mapOf("name" to "Jimmy"), mapOf("name" to "Bob") )))
   }

   it("allows calling an operation as the top-level entity") {
      val (vyne,stub) = testVyne("""
         model Person {
            name : PersonName inherits String
         }
         service PersonService {
            operation getName(emailAddress: String):Person
         }
      """.trimIndent())
      stub.addResponse("getName", """{ "name" : "Jimmy" }""")
      val result = vyne.query("""find { PersonService::getName("jimmy") }""")
         .firstRawObject()
      result.shouldBe(mapOf("name" to "Jimmy"))
   }

   it("errors if I try and call a stream operation") {
      val (vyne,stub) = testVyne("""
model Person {
   name : PersonName inherits String
}
service PersonService {
   stream peopleEvents: Stream<Person>
}
      """.trimIndent())
      val e = shouldThrow<IllegalArgumentException> {
         vyne.query(
            """
         find {
            person : Stream<Person> = PersonService::peopleEvents()
         }
      """.trimIndent()
         )
            .firstRawObject()
      }
      e.message.shouldBe("Operation peopleEvents returns a stream, which is not supported for in-place operation calls. Try querying with stream { Person }")
   }
})
