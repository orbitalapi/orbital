package com.orbitalhq.models.functions

import com.orbitalhq.firstRawObject
import com.orbitalhq.firstRawValue
import com.orbitalhq.firstTypedInstace
import com.winterbe.expekt.should
import com.orbitalhq.models.Provided
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.models.TypedObject
import com.orbitalhq.testVyne
import com.orbitalhq.typedInstances
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.runBlocking
import org.junit.Test

class FunctionEvaluatingAccessorReaderTest {

   @Test
   fun `default vyne should include stdlib`() {
      val (vyne,_) = testVyne("")
      val function = vyne.schema.taxi.function("taxi.stdlib.left")
   }
   @Test
   fun `functions should be invoked`() {
      val (vyne,_) = testVyne("""
         type Person {
            fullName : String by jsonPath("$.name")
            title : String by left(this.fullName,3)
         }
      """)
      val sourceJson = """{ "name" : "Mr. Jimmy" }"""
      val instance = TypedInstance.from(vyne.type("Person"), sourceJson, vyne.schema, source = Provided) as TypedObject
      instance["title"].value.should.equal("Mr.")
   }
   @Test
   fun `can evaluate function in find statement`():Unit = runBlocking {
      val (vyne, _) = testVyne("""
         type Message inherits String
      """.trimIndent())
      val result = vyne.query(
         """given { s:Message = "hello", w:Message = "world" } find {
         |message : Message by s
         |upperMessage : String by s.upperCase()
         |}""".trimMargin()
      )
         .firstRawObject()
      result.shouldBe(mapOf("message" to "hello", "upperMessage" to "HELLO"))
   }

   @Test
   fun `can evaluate function with body`():Unit = runBlocking {
      val (vyne, _) = testVyne("""
         extension function upperCut(input:String):String -> input.upperCase().trim()
      """.trimIndent())
      vyne.query("""given { s: String = ' Hello World ' } find { upperCut(s) } """)
         .firstRawValue()
         .shouldBe("HELLO WORLD")
   }
   @Test
   fun `can evaluate function with body as extension function`():Unit = runBlocking {
      val (vyne, _) = testVyne("""
         extension function upperCut(input:String):String -> input.upperCase().trim()
      """.trimIndent())
      vyne.query("""given { s: String = ' Hello World ' } find { s.upperCut() } """)
         .firstRawValue()
         .shouldBe("HELLO WORLD")
   }
   @Test
   fun `can evaluate function with body as field member using extension function`():Unit = runBlocking {
      val (vyne, _) = testVyne("""
         extension function upperCut(input:String):String -> input.upperCase().trim()
      """.trimIndent())
      vyne.query("""given { s: String = ' Hello World ' } find { s } as (s1:String) -> {
         | rawMessage : String by s1
         | tidiedMessage : String by s1.upperCut()
         |}
      """.trimMargin())
         .firstRawObject()
         .shouldBe(mapOf("rawMessage" to " Hello World ", "tidiedMessage" to "HELLO WORLD"))
   }
   @Test
   fun `can evaluate function with body as field member`():Unit = runBlocking {
      val (vyne, _) = testVyne("""
         extension function upperCut(input:String):String -> input.upperCase().trim()
      """.trimIndent())
      vyne.query("""given { s: String = ' Hello World ' } find { s } as (s1:String) -> {
         | rawMessage : String by s1
         | tidiedMessage : String by upperCut(s1)
         |}
      """.trimMargin())
         .firstRawObject()
         .shouldBe(mapOf("rawMessage" to " Hello World ", "tidiedMessage" to "HELLO WORLD"))
   }
   @Test
   fun `uses correctly scoped value for input when evaluating function body`():Unit = runBlocking {
      val (vyne, _) = testVyne("""
         type Message inherits String
         extension function upperCut(s:String):String -> s.upperCase().trim()

      """.trimIndent())
      val result = vyne.query(
         """given { s:Message = "hello", w:Message = "world" } find {
         |message : Message by s
         |upperMessage : String by w.upperCut()
         |}""".trimMargin()
      )
         .firstRawObject()
      result.shouldBe(mapOf("message" to "hello", "upperMessage" to "WORLD"))
   }

   // ORB-714
   @Test
   fun `can evaluate a nested expression`():Unit = runBlocking {
      val (vyne) = testVyne("""
         function compoundFilter(
           firstSet: String[],
           secondSet: String[]
         ):String[] -> firstSet.filter( (String) -> secondSet.contains('hello'))
      """.trimIndent())
      val result = vyne.query("""find { compoundFilter(
    ['a','b','hello'],
    ['hello']
)}""").typedInstances().map { it.toRawObject() }
      // The point of this test isn't the output, just that an error wasn't
      // thrown, as per ORB-714
      // However, let's be thorought
      result.shouldBe(listOf("a", "b", "hello"))
   }

   @Test
   fun `will evaluate a model selector expression as input`():Unit = runBlocking {
      val (vyne) = testVyne("""
         model Person {
            contactDetails: ContactDetails
         }
         model ContactDetails {
            address : Address
         }
         model Address {
            streetName : StreetName inherits String
         }

         model Thing {
            person : Person
            upperStreet: String = upperCase(this.person::ContactDetails::Address::StreetName)
         }
      """.trimIndent())
      val result = vyne.query("""
         given {
            person: Person = {
               contactDetails: {
                  address: {
                     streetName: "Oxford St"
                  }
               }
            }
         }
         find {
            street: String = upperCase(person::ContactDetails::Address::StreetName)
         }
      """.trimIndent())
         .firstRawObject()
      result.shouldBe(mapOf("street" to "OXFORD ST"))
   }

   @Test
   fun `will evaluate a model selector on a nested function expression as input`():Unit = runBlocking {
      val (vyne) = testVyne("""
         model Person {
            contactDetails: ContactDetails
         }
         model ContactDetails {
            address : Address
         }
         model Address {
            streetName : StreetName inherits String
         }

         model Thing {
            upperStreet: String
         }

         function upperCaseStreet(person:Person):Thing -> {
            upperStreet : upperCase(person::ContactDetails::Address::StreetName)
          }
      """.trimIndent())
      val result = vyne.query("""
         given {
            person: Person = {
               contactDetails: {
                  address: {
                     streetName: "Oxford St"
                  }
               }
            }
         }
         find {
            thing: Thing = upperCaseStreet(person)
         }
      """.trimIndent())
         .firstRawObject()
      result.shouldBe(mapOf("thing" to mapOf("upperStreet" to "OXFORD ST")))
   }

   @Test
   fun `will evaluate a model selector containing an expression type on a nested function expression as input`():Unit = runBlocking {
      val (vyne) = testVyne("""
         model Person {
            contactDetails: ContactDetails
         }
         model ContactDetails {
            addresses : Address[]
         }
         model Address {
            streetName : StreetName inherits String
            isPrimary : IsPrimaryAddress inherits Boolean
         }

         type PrimaryAddress inherits Address = Address[].single((IsPrimaryAddress) -> IsPrimaryAddress)

         model Thing {
            upperStreet: UpperStreet inherits String
         }

         function upperCaseStreet(person:Person):Thing -> {
            upperStreet : upperCase(person::ContactDetails::PrimaryAddress::StreetName)
          }
      """.trimIndent())
      val result = vyne.query("""
         given {
            person: Person = {
               contactDetails: {
                  addresses: [
                     { streetName: "Oxford St", isPrimary: true },
                     { streetName: "Regent St", isPrimary: false }
                  ]
               }
            }
         }
         find {
            thing: Thing = upperCaseStreet(person)
         }
      """.trimIndent())
         .firstRawObject()
      result.shouldBe(mapOf("thing" to mapOf("upperStreet" to "OXFORD ST")))
   }

   // ORB-1003
   @Test
   fun `can pass named arguments into a map function`():Unit = runBlocking {
      val (vyne) = testVyne("""
         model Person {
             name : Name inherits String
         }
         model Company {
             employees: Person[]
         }
         function upperCaseName(person:Person):Name -> person::Name.upperCase()
      """.trimIndent())
      vyne.query("""
         given { Company = {
             employees: [ { name: "Jimmy"}]
         }}
         find { Company } as {
             people: Name[] = Person[].map((p:Person) -> upperCaseName(p))
         }
      """.trimIndent())
         .firstRawObject()
         .shouldBe(mapOf("people" to listOf("JIMMY")))
   }
}


