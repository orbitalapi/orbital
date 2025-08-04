package com.orbitalhq

import com.orbitalhq.models.TypedObject
import com.orbitalhq.models.json.parseJson
import io.kotest.common.runBlocking
import io.kotest.matchers.shouldBe
import org.junit.Test
import java.math.BigInteger

class CastExpressionTest {

   @Test
   fun `can cast between compatible types`() {
      val (vyne,stub) = testVyne("""
         type ReleaseYear inherits Int
         type PublicDomainYear inherits Int
         model Movie {
            released : ReleaseYear
            isCopyright : Boolean
            publicDomain : PublicDomainYear = when {
               this.isCopyright -> this.released + 30
               else -> (PublicDomainYear) this.released
            }
         }
      """)
      val instance = vyne.parseJson("Movie", """{ "released" : 2020, "isCopyright" : true}""") as TypedObject
      instance["publicDomain"].value.shouldBe(2050)
      instance["publicDomain"].type.qualifiedName.shortDisplayName.shouldBe("PublicDomainYear")

      val instance2 = vyne.parseJson("Movie", """{ "released" : 2020, "isCopyright" : false}""") as TypedObject
      instance2["publicDomain"].value.shouldBe(2020)
      instance2["publicDomain"].type.qualifiedName.shortDisplayName.shouldBe("PublicDomainYear")
   }

   @Test
   fun `can use cast with attribute selector`():Unit = runBlocking {
      val (vyne) = testVyne("""
         type EntityId inherits String = (Film) -> (EntityId) Film::FilmId
         type FilmId inherits String

         model Film {
            filmId : FilmId
         }

         model Entity {
           entityId : EntityId
         }
      """.trimIndent())
      vyne.query("""
         given { film : Film = { filmId : "foo" } }
         find { Entity }
      """.trimIndent())
         .firstRawObject()
         .shouldBe(mapOf("entityId" to "foo"))
   }

   @Test
   fun `can cast using a plain type expression`():Unit = runBlocking {
      val (vyne,stub) = testVyne("""
         model Person {
            id: PersonId inherits String
         }
         service PersonService {
            operation getPeople():Person[]
         }
      """.trimIndent())
      stub.addResponse("getPeople", vyne.parseJson("Person[]", """[{"id" : "123"}]"""))
      val result = vyne.query("""
         find { Person[] } as {
            id : Long = (Long) PersonId
         }[]
      """.trimIndent())
         .firstRawObject()
      result
         .shouldBe(mapOf("id" to BigInteger.valueOf(123L)))
   }

   @Test
   fun `can cast a type alias`():Unit = runBlocking {
      val (vyne,stub) = testVyne("""
         type PersonName inherits String
         type Name inherits String
         type alias HumanName as PersonName
      """.trimIndent())
      vyne.query("""given { HumanName = 'Jimmy'}
find {
   name : (Name) PersonName
}""").firstRawObject()
         .shouldBe(mapOf("name" to "Jimmy"))
   }
}
