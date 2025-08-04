package com.orbitalhq

import io.kotest.matchers.shouldBe
import kotlinx.coroutines.runBlocking
import org.junit.Test

class FunctionTest {
   @Test
   fun `can pass default values to function`():Unit = runBlocking {
      val (vyne) = testVyne("""
         function sayHello(name: String, upperName:String = name.upperCase()):String -> concat(name, upperName)
      """.trimIndent())
      val result = vyne.query("""find { sayHello('jimmy') }""")
         .firstTypedInstace().toRawObject()
      result.shouldBe("jimmyJIMMY")
   }

   @Test
   fun `given two arrays as inputs into function they are correctly evaluated`():Unit = runBlocking {
      val (vyne,_) = testVyne("""
         function oneActor(films:Film[], actors:Actor[]):Actor -> actors.getAtIndex(0)
         function oneFilm(films:Film[], actors:Actor[]):Film -> films.getAtIndex(0)
         model Film {
            title : Title inherits String
         }
         model Actor {
            name : Name inherits String
         }
      """.trimIndent())
      val result = vyne.query("""
         given { actors: Actor[] = [ { name : "Jimmy" } ], films: Film[] = [ { title : "Jaws" } ] }
          find {
            actor : Actor = oneActor(films, actors)
            film : Film = oneFilm(films, actors)
         }
      """.trimIndent())
      .firstRawObject()
      result.shouldBe(mapOf(
         "actor" to mapOf("name" to "Jimmy"),
         "film" to mapOf("title" to "Jaws")
      ))
   }
}
