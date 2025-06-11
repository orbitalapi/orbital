package com.orbitalhq

import com.orbitalhq.models.AmbiguousResult
import com.orbitalhq.models.TypedNull
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
      it("applies default value when defined at type") {
         val (vyne, stub) = testVyne(
            """
            type PersonName inherits String by 'Always Jimmy'
            type PersonId inherits String

            model Person {
               id : PersonId
               name : PersonName
            }
            service PersonApi {
               operation getPerson():Person
            }
         """.trimIndent()
         )
         stub.addResponse("getPerson", """{ "id" : "123" }""")
         val result = vyne.query("""find { Person }""")
            .firstRawObject()
         result.shouldBe(mapOf("id" to "123", "name" to "Always Jimmy"))
      }

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
         vyne.query(
            """
         given { CustomerType = 'Retail' }
         find { AccountType }
      """.trimIndent()
         )
            .firstRawValue().shouldBe("Personal")
      }

      it("can use a scoped variable as an input to an expression function") {
         val (vyne, stub) = testVyne(
            """
            model Film {
               title : Title inherits String
               minAge : Age inherits Int
            }
            service FilmsService {
               operation getFilms():Film[]
            }
            type FirstAllowedFilmTitle by (Film[], viewerAge:Age) -> Film[].filter( (Film) -> Film::Age > viewerAge )
               .first()
               .convert(Title)
         """.trimIndent()
         )
         stub.addResponse(
            "getFilms",
            """[{"title" : "Star Wars", "minAge" : 8 }, {"title" : "Jaws" , "minAge" : 12 }]"""
         )
         val f = vyne.query("""given { Age = 6 } find { FirstAllowedFilmTitle }""")
            .typedInstances()
         f.shouldHaveSize(1)
         f.single().toRawObject().shouldBe("Star Wars")
      }

      it("is possible to use argument names in expression types") {
         val (vyne) = testVyne(
            """
            type Name inherits String
            type UppercaseName inherits String by (name:Name) -> name.upperCase()
         """
         )
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
            val (vyne, stub) = vyneWithExpressionType(""" type Adults by (Person[](Age > 18)) -> Person[].first()""")
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
            val (vyne, stub) = vyneWithExpressionType(""" type Adults by (age:Age) -> (Person[](Age > age)) -> Person[].first()""")
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
            val (vyne, stub) = vyneWithExpressionType(""" type Adults by (Person[](Age > 18)) -> Person[].first()""")
            stub.addTableFindManyResponse("people", """[{ "age" : 20}, {"age": 31 }]""")
            val result = vyne.query(
               """
               given { Message = "Hello" }
               find { Message } as {
                  message : Message
                  adults : Adults
               }"""
            ).firstRawObject()
            result.shouldBe(mapOf("message" to "Hello", "adults" to mapOf("age" to 20)))
            result.shouldNotBeNull()
            stub.calls["people_findManyPerson"].shouldHaveSize(1)
            val inputs = stub.calls["people_findManyPerson"].single()
            inputs.shouldHaveSize(1)
            inputs.single().value.shouldBeInstanceOf<String>()
               .removeNewLines()
               .shouldBe("""find { lang.taxi.Array<Person>(Age > 18) }""")

         }

         it("is possible to use selectors to refine scope") {
            val (vyne, stub) = testVyne(
               """
               type CaseId inherits String

               model Address {
                 line1: AddressLine1 inherits String
                 isCurrentAddress: IsCurrentAddress
               }

               type IsCurrentAddress inherits Boolean
               type IsPrimaryApplicant inherits Boolean

               type ActiveAddress inherits Address = (Address[]) -> Address[].single((IsCurrentAddress) -> IsCurrentAddress == true)

               model Individual {
                 addresses: Address[]
                 isPrimaryApplicant: IsPrimaryApplicant
               }

               model Case {
                 individuals: Individual[]
               }

               extension function primaryApplicant(applicants:Individual[]):Individual -> applicants.single((IsPrimaryApplicant) -> IsPrimaryApplicant == true)
               extension function secondaryApplicant(applicants:Individual[]):Individual -> applicants.single((IsPrimaryApplicant) -> IsPrimaryApplicant != true)


               service CaseService {
                 operation getCase(id: CaseId): Case
               }
            """.trimIndent()
            )
            stub.addResponse(
               "getCase", """{
    "individuals": [
        {
            "addresses": [
                {
                    "line1": "1 home st",
                    "isCurrentAddress": true
                },
                {
                    "line1": "1 old home st",
                    "isCurrentAddress": false
                }

            ],
            "isPrimaryApplicant": true
        },
        {
            "addresses": [
                {
                    "line1": "2 home st",
                    "isCurrentAddress": true
                },
                {
                    "line1": "2 old home st",
                    "isCurrentAddress": false
                }

            ],
            "isPrimaryApplicant": false
        }
    ]
}"""
            )
            val result = vyne.query(
               """given { CaseId = '123' }
find { Case } as (
    // two individuals in scope
    primaryApplicant: Individual[].primaryApplicant(),
    secondaryApplicant: Individual[].secondaryApplicant()
) -> {
    // This should correctly select the address array from the primary
    // applicant, but it returns null
    primaryApplicantAddresses: primaryApplicant::Address[]
    primaryActiveAddress: primaryApplicant::ActiveAddress
}"""
            ).firstTypedInstace()
            result.shouldNotBeNull()
//            result["primaryApplicantAddresses"].shouldBeInstanceOf<List<Map<String, Any>>>()
//               .shouldHaveSize(2)
         }

         it("evaluates a chained selector against the output of the previous expression") {
            val (vyne,stub) = testVyne("""
               model Person {
                  name : Name inherits String,
                  address: Address[]
               }
               model Address {
                  line1 : Line1 inherits String
                  active : Active inherits Boolean
               }
               type ActiveAddress inherits Address = (Address[]) -> Address[].single((Active) -> Active == true)
            """.trimIndent())
            val result = vyne.query("""
               given {
                  jim : Person = { name: "jim", address: [ { line1 : "1 old st", active : false } , { line1: "1 new st", active: true } ] },
                  jack : Person = { name: "jack", address: [ { line1 : "1 old st", active : false } , { line1: "1 new st", active: true } ] }
               }
               find {
               // The test is that Activeaddress is an expression type.
               // We need to ensure that when evaluating ActiveAddress, it's done against a child scope of jim,
               // so that we don't get multiple values in-scope
                  activeJim : jim::ActiveAddress
               }
            """.trimIndent())
               .firstRawObject()
            result.shouldBe(mapOf("activeJim" to mapOf("line1" to "1 new st",  "active" to true)))
         }

         // ORB-966
         it("handles a null value in a chain of expression types") {
            val (vyne,stub) = testVyne("""
type CaseId inherits String

model Address {
  line1: AddressLine1 inherits String
  isCurrentAddress: IsCurrentAddress
}

type IsCurrentAddress inherits Boolean
type IsPrimaryApplicant inherits Boolean

type ActiveAddress inherits Address = (Address[]) -> Address[].single((IsCurrentAddress) -> IsCurrentAddress == true)

model Individual {
  addresses: Address[]
  isPrimaryApplicant: IsPrimaryApplicant
}

model Case {
  individuals: Individual[]
}

extension function secondaryApplicant(applicants:Individual[]):Individual -> applicants.single((IsPrimaryApplicant) -> IsPrimaryApplicant != true)

service CaseService {
  operation getCase(id: CaseId): Case
}
            """.trimIndent())
            stub.addResponse("getCase", """{
    "individuals": [
        {
            "addresses": [
                {
                    "line1": "1 home st",
                    "isCurrentAddress": true
                },
                {
                    "line1": "1 old home st",
                    "isCurrentAddress": false
                }

            ],
            "isPrimaryApplicant": true
        }

    ]
}""")
            val result = vyne.query("""
               given { CaseId = '123' }
               find { Case } as (secondary: Individual[].secondaryApplicant()) -> {
                   addressLine1: secondary::ActiveAddress::AddressLine1
               }
            """.trimIndent())
               .firstRawObject()
            result.shouldBe(mapOf("addressLine1" to null))
         }

         it("should return null when type cast is ambiguous") {
            val (vyne,stub) = testVyne("""
               closed model Address {
                 line1: AddressLine1 inherits String
               }
               closed model Individual {
                 addresses: Address[]
               }
               closed model Case {
                 individuals: Individual[]
               }
               service CaseService {
                 operation getCase(): Case
               }
            """.trimIndent())
            stub.addResponse("getCase","""{
    "individuals": [
        {
            "addresses": [
                {
                    "line1": "1 home st"
                },
                {
                    "line1": "1 old home st"
                }
            ]
        }
    ]
}""")
            val result = vyne.query("""find { Case } as {
    // This should return null
    line1: Individual as AddressLine1
}""").firstTypedObject()
            result.toRawObject().shouldBe(mapOf("line1" to null))
            val line1 = result.get("line1").shouldBeInstanceOf<TypedNull>()
            line1.source.shouldBeInstanceOf<AmbiguousResult>()
         }
      }
   }
})
