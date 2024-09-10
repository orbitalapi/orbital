package com.orbitalhq.query.policyManager

import com.orbitalhq.AuthClaimType
import com.orbitalhq.FactSets
import com.orbitalhq.Vyne
import com.orbitalhq.errors.ErrorType
import com.orbitalhq.errors.OrbitalQueryException
import com.orbitalhq.firstRawObject
import com.orbitalhq.firstTypedInstace
import com.orbitalhq.firstTypedObject
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.models.TypedNull
import com.orbitalhq.models.TypedObject
import com.orbitalhq.models.json.parseJson
import com.orbitalhq.query.Fact
import com.orbitalhq.query.QueryResult
import com.orbitalhq.rawObjects
import com.orbitalhq.stubbing.StubService
import com.orbitalhq.testVyne
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.runBlocking
import lang.taxi.utils.quotedIfNotAlready
import org.junit.jupiter.api.assertThrows

class PolicyEvaluationSpec : DescribeSpec({
   describe("policy evaluation") {
      val baseSchema = """
         type DvdReleaseDate inherits Int
         type Sensitive

         type Title inherits String, Sensitive

         model Film {
            title : Title
            yearReleased : YearReleased inherits Int
         }

         model UserInfo inherits com.orbitalhq.auth.AuthClaims {
            groups : SecurityGroup[]
            userId : UserId inherits String
         }

         type SecurityGroup inherits String

         service FilmsService {
            operation getOneFilm():Film
            operation getManyFilms():Film[]
         }
      """.trimIndent()

      fun runQueryWithPolicy(
         policySchema: String,
         query: String,
         userRole: String = "ADMIN"
      ): QueryResult {
         val (vyne, stub) = testVyne(
            AuthClaimType.AuthClaimsTypeDefinition,
            """
         $baseSchema

         $policySchema
         """
         )
         addStubs(stub, vyne)
         val user = vyne.userWithRole(userRole)
         return runBlocking { vyne.query(query, executionContextFacts = setOf(user)) }
      }


      it("is possible to suppress a field using a policy") {
         val (vyne, stub) = testVyne(
            AuthClaimType.AuthClaimsTypeDefinition,
            """
         $baseSchema

         policy AllAccessFilms against Film {
            read { Film as { ... except { yearReleased } } }
         }
         """
         )
         addStubs(stub, vyne)
         val first = vyne.query("""find { Film }""")
            .firstRawObject()
         // A policy cannot alter structural contracts - only values. So
         // yearReleased becomes populated with null
         first.shouldBe(mapOf("title" to "Star Wars", "yearReleased" to null))
      }


      it("is possible to suppress a field based on a user property") {
         val policy = """
            policy FilterYearReleased against Film (userInfo : UserInfo) -> {
               read {
                  when {
                     userInfo.groups.contains( 'ADMIN' ) -> Film
                     else -> Film as { ... except { yearReleased } }
                  }
               }
            }"""
         val adminResult = runQueryWithPolicy(
            policy,
            query = "find { Film }",
            userRole = "ADMIN"
         ).firstRawObject()
         adminResult.shouldBe(mapOf("title" to "Star Wars", "yearReleased" to 1978))

         val normalResult = runQueryWithPolicy(
            policy,
            query = "find { Film }",
            userRole = "USER"
         ).firstRawObject()
         normalResult.shouldBe(mapOf("title" to "Star Wars", "yearReleased" to null))
      }

      it("is possible to define a policy against a nullable type that returns null") {
         val policy = """
            policy ThrowErrorIfFilmIsNull against Film (film:Film?) -> {
               read {
                  when {
                     film == null -> throw( (NotAuthorizedError) { message: 'Not Authorized' })
                     else -> Film
                  }
               }
            }
         """.trimIndent()
         val (vyne, stub) = testVyne(
            ErrorType.ErrorTypeDefinition,
            AuthClaimType.AuthClaimsTypeDefinition,
            """
         $baseSchema
         $policy
         """
         )
         stub.addResponse("getOneFilm", TypedNull.create(vyne.schema.type("Film")))
         val exception = assertThrows<OrbitalQueryException> {
            val result = vyne
               .query("""find { Film }""")
               .firstRawObject()
         }
         exception.shouldNotBeNull()
         exception.message.shouldBe("Not Authorized")
      }

      it("is possible to throw an error from a policy") {
         val (vyne, stub) = testVyne(
            AuthClaimType.AuthClaimsTypeDefinition,
            ErrorType.ErrorTypeDefinition,
            """
         $baseSchema

         policy AllAccessFilms against Film (userInfo : UserInfo) -> {
            read {
               when {
                  userInfo.groups.contains( 'ADMIN' ) -> Film
                  else -> throw( (NotAuthorizedError) { message: 'Not Authorized' })
               }
            }
         }
         """
         )
         addStubs(stub, vyne)
         val user = vyne.userWithRole("USER")
         val exception = assertThrows<OrbitalQueryException> {
            val adminResult = vyne
               .query("""find { Film }""", executionContextFacts = setOf(user))
               .firstRawObject()
         }
         exception.message.shouldBe("Not Authorized")
      }

      it("can enforce a policy on a nested type") {
         val (vyne, stub) = testVyne(
            AuthClaimType.AuthClaimsTypeDefinition,
            """
         $baseSchema

         policy FilterFilmTitle against Title (userInfo : UserInfo) -> {
            read {
               when {
                  userInfo.groups.contains( 'ADMIN' ) -> Title
                  else -> concat(left(Title,3), "***")
               }
            }
         }
         """
         )
         addStubs(stub, vyne)
         val normalUser = vyne.userWithRole("USER")

         val listResultWithPolicyApplied = vyne
            .query("""find { Film[] }""", executionContextFacts = setOf(normalUser))
            .rawObjects()
         listResultWithPolicyApplied.shouldBe(
            listOf(
               mapOf(
                  "title" to "Sta***",
                  "yearReleased" to 1978
               ),
               mapOf(
                  "title" to "Emp***",
                  "yearReleased" to 1982
               )
            ),
         )
      }

      it("applies a policy when the supplied value is null") {
         val (vyne, stub) = testVyne(
            AuthClaimType.AuthClaimsTypeDefinition,
            """
         $baseSchema

         policy FilterFilmTitle against Title (userInfo : UserInfo) -> {
            read {
               when {
                  userInfo.groups.contains( 'ADMIN' ) -> Title
                  else -> null
               }
            }
         }
         """
         )
         addStubs(stub, vyne)
         stub.addResponse("getOneFilm", """{ "title" : null, "yearReleased" : 1978 }""")
         val listResultWithoutPolicyApplied = vyne
            .query("""find { Film }""", executionContextFacts = setOf(vyne.userWithRole("ADMIN")))
            .firstRawObject()
            .shouldBe(mapOf("title" to null, "yearReleased" to 1978))

         val listResultWithPolicyApplied = vyne
            .query("""find { Film }""", executionContextFacts = setOf(vyne.userWithRole("USER")))
            .firstRawObject()
            .shouldBe(mapOf("title" to null, "yearReleased" to 1978))

      }

      it("applies a policy defined against a base type and the supplied value is null") {
         val (vyne, stub) = testVyne(
            AuthClaimType.AuthClaimsTypeDefinition,
            """
         $baseSchema

         policy FilterFilmTitle against Sensitive (userInfo : UserInfo) -> {
            read {
               when {
                  userInfo.groups.contains( 'ADMIN' ) -> Sensitive
                  else -> null
               }
            }
         }
         """
         )
         addStubs(stub, vyne)
         stub.addResponse("getOneFilm", """{ "title" : null, "yearReleased" : 1978 }""")
         val listResultWithoutPolicyApplied = vyne
            .query("""find { Film }""", executionContextFacts = setOf(vyne.userWithRole("ADMIN")))
            .firstRawObject()
            .shouldBe(mapOf("title" to null, "yearReleased" to 1978))

         val listResultWithPolicyApplied = vyne
            .query("""find { Film }""", executionContextFacts = setOf(vyne.userWithRole("USER")))
            .firstRawObject()
            .shouldBe(mapOf("title" to null, "yearReleased" to 1978))

      }




      it("when applying a policy that modifies an input to an expression, the expression is impacted") {
         val (vyne, stub) = testVyne(
            AuthClaimType.AuthClaimsTypeDefinition,
            """
         $baseSchema

         policy AllAccessFilms against Title (userInfo : UserInfo) -> {
            read {
               when {
                  userInfo.groups.contains( 'ADMIN' ) -> Title
                  else -> concat(left(Title,3), "***")
               }
            }
         }
         """
         )
         addStubs(stub, vyne)
         val normalUser = vyne.userWithRole("USER")
         val normalResult = vyne
            .query("""find { Film }""", executionContextFacts = setOf(normalUser))
            .firstRawObject()
      }

      it("can load data from a remote service as part of decision") {
         val (vyne, stub) = testVyne(
            AuthClaimType.AuthClaimsTypeDefinition,
            """
         $baseSchema

         model UserConsent {
            acceptedTermsAndConditions : AcceptedTermsAndConditions inherits Boolean
         }

         service UserService {
            operation getConsent(UserId):UserConsent
         }

         policy AllAccessFilms against Film (userInfo : UserInfo, acceptedTerms: AcceptedTermsAndConditions) -> {
            read {
               when {
                  acceptedTerms == false -> null
                  else -> Film
               }
            }
         }
         """
         )
         addStubs(stub, vyne)
         fun stubUserHasAcceptedTermsAndConditions(value: Boolean) {
            stub.addResponse(
               "getConsent",
               vyne.parseJson("UserConsent", """{ "acceptedTermsAndConditions" : $value }""")
            )
         }
         stubUserHasAcceptedTermsAndConditions(false)
         val filteredResult = vyne
            .query("find { Film }", executionContextFacts = setOf(vyne.userWithRole("USER")))
            .firstTypedInstace()

         filteredResult.value.shouldBeNull()

         stubUserHasAcceptedTermsAndConditions(true)

         val unfilteredResult = vyne
            .query("find { Film }", executionContextFacts = setOf(vyne.userWithRole("USER")))
            .firstTypedInstace()

         val rawObject = unfilteredResult.shouldBeInstanceOf<TypedObject>()
            .toRawObject() as Map<String, Any>
         rawObject.shouldBe(mapOf("title" to "Star Wars", "yearReleased" to 1978))

      }

      it("can perform a simple projection on a type with a policy applied on a field") {
         val policy = """
            policy FilterYearReleased against Title (userInfo : UserInfo) -> {
               read {
                  when {
                     userInfo.groups.contains( 'ADMIN' ) -> Title
                     else -> "***"
                  }
               }
            }"""
         val userResult = runQueryWithPolicy(
            policy,
            query = """find { Film } as {
               |name : Title
               |}
            """.trimMargin(),
            userRole = "USER"
         ).firstRawObject()
         userResult.shouldBe(mapOf("name" to "***"))
      }

      it("when attempting to project and use a field that has been removed by a policy then returns null") {
         val policy = """
            policy FilterYearReleased against Film (userInfo : UserInfo) -> {
               read {
                  when {
                     userInfo.groups.contains( 'ADMIN' ) -> Film
                     else -> Film as { ... except { title } }
                  }
               }
            }"""
         val userResult = runQueryWithPolicy(
            policy,
            query = """find { Film } as {
               |name : Title
               |}
            """.trimMargin(),
            userRole = "USER"
         ).firstRawObject()
         userResult.shouldBe(mapOf("name" to null))
      }


      it("does not re-load suppressed data from another service") {
         val (vyne, stub) = testVyne(
            """
            model Film {
               id : FilmId inherits Int
               title : Title inherits String
            }
            service FilmsApi {
               operation getAll():Film[]
               operation getOne(FilmId):Film
            }
            policy SuppressTitle against Film {
               read {
                  Film as { ... except { title } }
               }
            }
         """.trimIndent()
         )
         stub.addResponse(
            "getAll",
            vyne.parseJson(
               "Film[]",
               """[{"id" : 1, "title" : "Jaws"}, {"id" : 2, "title" : "Aliens"}, {"id" : 3, "title" : "Star Wars"}]"""
            ),
            modifyDataSource = true
         )
         stub.addResponse("getOne", vyne.parseJson("Film", """{"id" : 1, "title" : "Jaws"}"""), modifyDataSource = true)

         val film = vyne.query("""find { Film[] }""")
            .firstRawObject()
         film.shouldBe(mapOf("id" to 1, "title" to null))
         stub.calls["getOne"].shouldBeEmpty()
         stub.calls["getAll"].shouldHaveSize(1)
      }

      it("can use expression values as input to a policy") {
         val policy =
            """policy FilterYearReleased against Film (userInfo : UserInfo, uppercaseUsername:String = upperCase(UserId), firstSecurityGroup:SecurityGroup = first(SecurityGroup[])) -> {
               read {
                  when {
                     firstSecurityGroup == 'ADMIN' && uppercaseUsername == 'JIMMY' -> Film
                     else -> Film as { ... except { title } }
                  }
               }
            }

         """.trimIndent()
         runQueryWithPolicy(
            policy,
            query = """find { Film }""",
            userRole = "ADMIN"
         ).firstRawObject()
            .shouldBe(mapOf("title" to "Star Wars", "yearReleased" to 1978))

         runQueryWithPolicy(
            policy,
            query = """find { Film }""",
            userRole = "USER"
         ).firstRawObject()
            .shouldBe(mapOf("title" to null, "yearReleased" to 1978))
      }

      it("can use scoped data on an expression type") {
         val (vyne, stub) = testVyne(
            """
            model Film {
               title : Title inherits String
               yearReleased : YearReleased inherits Int
            }
            model TermsAndConditionsAcceptance {
               acceptedDate : AcceptedDate inherits Date
               validUntil : ValidUntil inherits Date
            }
            service FilmsApi {
               operation getFilm():Film
               operation loadUserTermsAndConditionsAcceptance():TermsAndConditionsAcceptance[]
            }

            type HasValidAcceptance by (AcceptedDate, ValidUntil) -> AcceptedDate < currentDate() && ValidUntil > currentDate()


            policy SuppressTitle against Film (acceptedTerms:TermsAndConditionsAcceptance = first(TermsAndConditionsAcceptance[])) -> {
               read {
                  when {
                     // This is the test.
                     // HasValidAcceptance can only be calculated using a single TermsAndConditionsAcceptance, which should
                      // resolve from the single TermsAndConditionsAcceptance that's in scope.
                     HasValidAcceptance -> Film
                     else -> Film as { ... except { yearReleased } }
                  }
               }
            }
         """.trimIndent()
         )
         // The first acceptance is valid, so should return all the data
         stub.addResponse("loadUserTermsAndConditionsAcceptance", """[
            |{ "acceptedDate" : "2019-01-01", "validUntil" : "2037-10-10" },
            |{ "acceptedDate" : "2017-01-01", "validUntil" : "2017-10-10" }
            |]""".trimMargin())
         stub.addResponse("getFilm", """{ "title" : "Star Wars", "yearReleased" : 1978  }""")
         vyne.query("""find { Film }""")
            .firstRawObject()
            .shouldBe(mapOf("title" to "Star Wars", "yearReleased" to 1978))

         // The first acceptance is invalid, so should filter the data
         stub.addResponse("loadUserTermsAndConditionsAcceptance", """[
            |{ "acceptedDate" : "2017-01-01", "validUntil" : "2017-10-10" },
            |{ "acceptedDate" : "2019-01-01", "validUntil" : "2037-10-10" }
            |]""".trimMargin())
         vyne.query("""find { Film }""")
            .firstRawObject()
            .shouldBe(mapOf("title" to "Star Wars", "yearReleased" to  null))
      }

      it("does not re-load suppressed data from another service during a projection") {
         val (vyne, stub) = testVyne(
            """
            model Film {
               id : FilmId inherits Int
               title : Title inherits String
            }
            service FilmsApi {
               operation getAll():Film[]
               operation getOne(FilmId):Film
            }
            policy SuppressTitle against Film {
               read {
                  Film as { ... except { title } }
               }
            }
         """.trimIndent()
         )
         stub.addResponse(
            "getAll",
            vyne.parseJson("Film[]", """[{"id" : 1, "title" : "Jaws"}]"""),
            modifyDataSource = true
         )
         stub.addResponse("getOne", vyne.parseJson("Film", """{"id" : 1, "title" : "Jaws"}"""), modifyDataSource = true)

         val film = vyne.query(
            """find { Film[] } as {
            |   name : Title
            |   id : FilmId
            |}[]
         """.trimMargin()
         )
            .firstRawObject()
         film.shouldBe(mapOf("name" to null, "id" to 1))
         stub.calls["getOne"].shouldBeEmpty()
      }
   }


})


fun Vyne.userWithRole(userId: String, roles: List<String>): TypedInstance {
   val json = """{ "userId" : "$userId", "groups" : [ ${roles.joinToString(",").quotedIfNotAlready()} ]}"""
   return this.parseJson("UserInfo", json)
}

fun Vyne.userWithRole(vararg role: String): Fact {
   return Fact.fromTypedInstance(userWithRole("jimmy", role.toList()), FactSets.AUTHENTICATION)
}

fun addStubs(stub: StubService, vyne: Vyne) {
   stub.addResponse("getOneFilm", vyne.parseJson("Film", """{ "title" : "Star Wars", "yearReleased" : 1978 }"""))
   val arrayResponse = vyne.parseJson(
      "Film[]",
      """[{ "title" : "Star Wars", "yearReleased" : 1978 }, {"title" : "Empire Strikes Back", "yearReleased" : 1982}]"""
   )
   stub.addResponse(
      "getManyFilms",
      arrayResponse
   )
}

