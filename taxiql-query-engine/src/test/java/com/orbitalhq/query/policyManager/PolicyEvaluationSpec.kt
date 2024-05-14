package com.orbitalhq.query.policyManager

import com.orbitalhq.JWTClaimType
import com.orbitalhq.StubService
import com.orbitalhq.Vyne
import com.orbitalhq.firstRawObject
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.models.json.parseJson
import com.orbitalhq.testVyne
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import lang.taxi.utils.quotedIfNotAlready

class PolicyEvaluationSpec : DescribeSpec({
   /**
    * These tests aren't ready yet - they currently throw a stack overflow exception.
    */
   xdescribe("policy evaluation") {
      val baseSchema = """
         model Film {
            title : Title inherits String
            yearReleased : YearReleased inherits Int
         }

         model UserInfo inherits com.orbitalhq.JwtClaim {
            groups : SecurityGroup[]
         }

         service FilmsService {
            operation getOneFilm():Film
            operation getManyFilms():Film[]
         }
      """.trimIndent()
      it("is possible to suppress a field using a policy") {
         val (vyne, stub) = testVyne(JWTClaimType.AuthClaimsTypeDefinition,
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
            JWTClaimType.AuthClaimsTypeDefinition,
            """
         $baseSchema

         type SecurityGroup inherits String

         policy AllAccessFilms against Film (userInfo : UserInfo) -> {
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
   }
})

fun Vyne.userWithRole(vararg role: String):TypedInstance {
   return this.parseJson("UserInfo", """{ "groups" : [ ${role.joinToString(",").quotedIfNotAlready()} ]}""")
}

fun addStubs(stub: StubService, vyne:Vyne) {
   stub.addResponse("getOneFilm", vyne.parseJson("Film", """{ "title" : "Star Wars", "yearReleased" : 1978 }"""))
   stub.addResponse("getManyFilms", vyne.parseJson("Film", """[{ "title" : "Star Wars", "yearReleased" : 1978 }, {"title" : "Empire Strikes Back", "yearReleased" : 1982}"""))
}
