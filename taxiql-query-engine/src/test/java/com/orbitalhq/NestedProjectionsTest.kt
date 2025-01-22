package com.orbitalhq

import io.kotest.matchers.shouldBe
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

class NestedProjectionsTest {


   // ORB-877
   @Test
   fun `can use a scope with expression result as input to discovery call`():Unit = runBlocking {
      val (vyne,stub) = testVyne("""
         model Actor {
            id : ActorId inherits String
         }
         model ActorDetails {
            fullName : ActorName  inherits String
         }
         model Film {
            id : FilmId inherits String
         }
         model FilmCast {
            actors : Actor[]
         }
         service FilmsApi {
            operation getFilm():Film
            operation getCast(FilmId):FilmCast
            operation getActor(ActorId):ActorDetails
         }
      """.trimIndent())

      stub.addResponse("getFilm", """{ "id" : "film1" }""")
      stub.addResponse("getCast", """{ "actors" : [ { "id" : "123" } ] }""")
      stub.addResponse("getActor", """{ "fullName" : "Jimmy" }""")

      val result = vyne.query("""find { Film } as {
         | // This is the test...
         | // first(Actor[]) should return an Actor, with an Id
         | // which is used to discover actor details.
         | // Previous, this wasn't working
         | leadActor: FilmCast as (first(Actor[])) -> {
         |    name : ActorName
         | }
         |}
      """.trimMargin())
         .firstRawObject()
      result.shouldBe(mapOf("leadActor" to mapOf("name" to "Jimmy")))
   }

}
