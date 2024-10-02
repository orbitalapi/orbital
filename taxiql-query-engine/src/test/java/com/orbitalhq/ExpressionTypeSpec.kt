package com.orbitalhq

import com.orbitalhq.query.VyneQlGrammar
import com.orbitalhq.utils.removeNewLines
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import lang.taxi.compiled

class ExpressionTypeSpec : DescribeSpec({

   describe("Expression types") {
      it("evaluates when clause on an expression type correctly") {
         val (vyne) = testVyne(
            """
      type CustomerType inherits String
      type AccountType inherits String by when(lowerCase(CustomerType)) {
           'retail'  -> 'Personal'
           'sme' -> 'Personal'
           else -> 'Business'
      }
   """
         )
         vyne.query("""
         given { CustomerType = 'Retail' }
         find { AccountType }
      """.trimIndent())
            .firstRawValue().shouldBe("Personal")
      }

      it("can use a scoped variable as an input to an expression function") {
         val (vyne,stub) = testVyne("""
            model Film {
               title : Title inherits String
               minAge : Age inherits Int
            }
            service FilmsService {
               operation getFilms():Film[]
            }
            type AllowedFilms by (Film[], viewerAge:Age) -> Film[].filter( (Film) -> Film::Age > viewerAge )
               .convert(Title)
         """.trimIndent())
         stub.addResponse("getFilms", """[{"title" : "Star Wars", "minAge" : 8 }, {"title" : "Jaws" , "minAge" : 12 }]""")
       val f=  vyne.query("""given { Age = 6 } find { AllowedFilms }""")
            .typedInstances()
         f.shouldNotBeNull()
      }

      it("is possible to use argument names in expression types") {
        val (vyne) = testVyne("""
            type Name inherits String
            type UppercaseName inherits String by (name:Name) -> name.upperCase()
         """)
         vyne.query("""given { Name = 'Jimmy' } find { UppercaseName }""")
            .firstRawValue()
            .shouldBe("JIMMY")
      }

      describe("constraints on expression type inputs") {

         fun vyneWithExpressionType(definition: String) = testVyne(
            VyneQlGrammar.QUERY_TYPE_TAXI,
            """
            closed model Person {
               age : Age inherits Int
            }
            type Message inherits String
            service PersonService {
               table people : Person[]
            }
            """,
            definition
         )

         it("is possible to use constraints on an expression type input") {
            val (vyne,stub) = vyneWithExpressionType(""" type Adults by (Person[](Age > 18)) -> Person[].first()""")
            stub.addTableFindManyResponse("people", """[{ "age" : 20}, {"age": 31 }]""")

            val result = vyne.query("""find { Adults }""")
               .rawObjects()
            result.shouldHaveSize(1)
            result.single().shouldBe(mapOf("age" to 20))
            stub.calls["people_findManyPerson"].shouldHaveSize(1)
            val inputs = stub.calls["people_findManyPerson"].single()
            inputs.shouldHaveSize(1)
            inputs.single().value.shouldBeInstanceOf<String>()
               .removeNewLines()
               .shouldBe("""find { lang.taxi.Array<Person>(Age > 18) }""")
         }
         it("is possible to use constraints on an expression type input with an input from a given clause") {
            // This is a gnarly example
            // Its multiple nested expressions with inputs, one which gets resolved as a scoped variable
            val (vyne,stub) = vyneWithExpressionType(""" type Adults by (age:Age) -> (Person[](Age > age)) -> Person[].first()""")
            stub.addTableFindManyResponse("people", """[{ "age" : 20}, {"age": 31 }]""")

            val result = vyne.query("""given { theAge:Age = 18 } find { Adults }""")
               .rawObjects()
            result.shouldHaveSize(1)
            result.single().shouldBe(mapOf("age" to 20))
            stub.calls["people_findManyPerson"].shouldHaveSize(1)
            val inputs = stub.calls["people_findManyPerson"].single()
            inputs.shouldHaveSize(1)
            inputs.single().value.shouldBeInstanceOf<String>()
               .removeNewLines()
               .shouldBe("""find { lang.taxi.Array<Person>(Age > 18) }""")
         }
         it("is possible to use an expression type with constraints on a model field") {
            val (vyne,stub) = vyneWithExpressionType(""" type Adults by (Person[](Age > 18)) -> Person[].first()""")
            stub.addTableFindManyResponse("people", """[{ "age" : 20}, {"age": 31 }]""")
            val result = vyne.query("""
               given { Message = "Hello" }
               find { Message } as {
                  message : Message
                  adults : Adults
               }""").firstRawObject()
            result.shouldBe(mapOf("message" to "Hello", "adults" to mapOf("age" to 20)))
            result.shouldNotBeNull()
            stub.calls["people_findManyPerson"].shouldHaveSize(1)
            val inputs = stub.calls["people_findManyPerson"].single()
            inputs.shouldHaveSize(1)
            inputs.single().value.shouldBeInstanceOf<String>()
               .removeNewLines()
               .shouldBe("""find { lang.taxi.Array<Person>(Age > 18) }""")

         }

      }
   }
})
