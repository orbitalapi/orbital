package com.orbitalhq.query.policyManager

import com.orbitalhq.AuthClaimType
import com.orbitalhq.Vyne
import com.orbitalhq.errors.ErrorType
import com.orbitalhq.errors.OrbitalQueryException
import com.orbitalhq.firstRawObject
import com.orbitalhq.firstTypedInstace
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.models.TypedObject
import com.orbitalhq.models.json.parseJson
import com.orbitalhq.rawObjects
import com.orbitalhq.stubbing.StubService
import com.orbitalhq.testVyne
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import lang.taxi.utils.quotedIfNotAlready
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.fail

class PolicyEvaluationSpec : DescribeSpec({
   describe("policy evaluation") {
      val baseSchema = """
         type DvdReleaseDate inherits Int
         model Film {
            title : Title inherits String
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
         first.shouldBe(mapOf("title" to "Star Wars"))
      }


      it("is possible to suppress a field based on a user property") {
         val (vyne, stub) = testVyne(
            AuthClaimType.AuthClaimsTypeDefinition,
            """
         $baseSchema



         policy FilterYearReleased against Film (userInfo : UserInfo) -> {
            read {
               when {
                  userInfo.groups.contains( 'ADMIN' ) -> Film
                  else -> Film as { ... except { yearReleased } }
               }
            }
         }
         """
         )
         addStubs(stub, vyne)
         val adminUser = vyne.userWithRole("ADMIN")
         val adminResult = vyne
            .addModel(adminUser)
            .query("""find { Film }""")
            .firstRawObject()
         adminResult.shouldBe(mapOf("title" to "Star Wars", "yearReleased" to 1978))

         val normalUser = vyne.userWithRole("USER")
         val normalResult = vyne
            .removeModel(adminUser)
            .addModel(normalUser)
            .query("""find { Film }""")
            .firstRawObject()
         normalResult.shouldBe(mapOf("title" to "Star Wars"))
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
               .addModel(user)
               .query("""find { Film }""")
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
//         val resultWithPolilyApplied = vyne
//            .addModel(normalUser)
//            .query("""find { Film }""")
//            .firstRawObject()
//         resultWithPolilyApplied.shouldBe(
//            mapOf(
//               "title" to "Sta***",
//               "yearReleased" to 1978
//            ),
//         )

         val listResultWithPolicyApplied = vyne
            .addModel(normalUser)
            .query("""find { Film[] }""")
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
            .addModel(normalUser)
            .query("""find { Film }""")
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
         val filteredResult = vyne.addModel(vyne.userWithRole("USER"))
            .query("find { Film }")
            .firstTypedInstace()

         filteredResult.value.shouldBeNull()

         stubUserHasAcceptedTermsAndConditions(true)

         val unfilteredResult = vyne.addModel(vyne.userWithRole("USER"))
            .query("find { Film }")
            .firstTypedInstace()

         val rawObject = unfilteredResult.shouldBeInstanceOf<TypedObject>()
            .toRawObject() as Map<String, Any>
         rawObject.shouldBe(mapOf("title" to "Star Wars", "yearReleased" to 1978))

      }
   }
})

fun Vyne.userWithRole(userId: String, roles: List<String>): TypedInstance {
   val json = """{ "userId" : "$userId", "groups" : [ ${roles.joinToString(",").quotedIfNotAlready()} ]}"""
   return this.parseJson("UserInfo", json)
}

fun Vyne.userWithRole(vararg role: String): TypedInstance {
   return userWithRole("jimmy", role.toList())
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
