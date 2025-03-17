package com.orbitalhq

import app.cash.turbine.test
import arrow.core.Either
import com.orbitalhq.models.TypedCollection
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.models.json.parseJson
import com.orbitalhq.models.json.tryParseJson
import com.orbitalhq.query.StreamErrorMessage
import io.kotest.common.runBlocking
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldStartWith
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.toList
import org.junit.jupiter.api.Test
import kotlin.time.Duration

class ExtensionFunctionTest {

   @Test
   fun `can filter results using expression`(): Unit = runBlocking {

      val (vyne, stub) = testVyne(
         """
            model Movie {
               title : Title inherits String
            }
            service Movies {
               operation findAll():Movie[]
            }
         """.trimIndent()
      )
      stub.addResponse("findAll", vyne.parseJson("Movie[]", """[{ "title" : "Jaws"}, {"title": "Star Wars"}]"""))

      val results = vyne.query(
         """
         find { Movie[].filter( (Title) -> Title == "Jaws" ) }
      """.trimIndent()
      )
         .rawObjects()

      results.shouldBe(
         listOf(
            mapOf("title" to "Jaws")
         )
      )
   }

   @Test
   fun `can use a simple extension function`(): Unit = runBlocking {
      val (vyne, stub) = testVyne("")
      vyne.query("""given { s:String = "hello" } find { s.upperCase() }""")
         .firstRawValue().shouldBe("HELLO")
   }

   @Test
   fun `can use a simple extension function on property`(): Unit = runBlocking {
      val (vyne, stub) = testVyne("type Message inherits String")
      val result = vyne.query(
         """given { s:Message = "hello", w:Message = "world" } find {
         |message : Message by s
         |upperMessage : String by w.upperCase().left(2)
         |}""".trimMargin()
      )
         .firstRawObject()
      result.shouldBe(
         mapOf(
            "message" to "hello",
            "upperMessage" to "WO"
         )
      )
   }

   @Test
   fun `can filter stream results using expression`(): Unit = runBlocking {
      val (vyne, stub) = testVyne(
         """
            closed model Movie {
               title : Title inherits String
            }
            service Movies {
               operation streamMovies():Stream<Movie>
            }
         """.trimIndent()
      )
      val moviesFlow = MutableSharedFlow<Either<StreamErrorMessage, TypedInstance>>(2)
      val starWars = vyne.tryParseJson("Movie", """{ "title" : "Star Wars"}""")
      moviesFlow.emit(starWars)
      moviesFlow.emit(vyne.tryParseJson("Movie", """{ "title" : "Jaws"}"""))
      stub.addResponseFlow("streamMovies") { _, _ -> moviesFlow }

      val results = vyne.query(
         """
         stream { Movie.filterEach( (Title) -> Title == "Jaws" ) }
      """
      ).results

      results.test(timeout = Duration.parse("15s")) {
         val next = expectTypedObject()
            .toRawObject()
         next.shouldBe(mapOf("title" to "Jaws"))
      }
   }

   @Test // ORB-650
   fun `can chain extension functions together`(): Unit = runBlocking {
      val (vyne, stub) = testVyne(
         """
            model Movie {
               title : Title inherits String
            }
            service Movies {
               operation findAll():Movie[]
            }
         """.trimIndent()
      )
      stub.addResponse("findAll", vyne.parseJson("Movie[]", """[{ "title" : "Jaws"}, {"title": "Star Wars"}]"""))

      val results = vyne.query(
         """
         find { Movie[].filter( (Title) -> Title == "Jaws" ).convert(Title) }
      """.trimIndent()
      ).rawResults.toList()


      results.shouldBe(
         listOf("Jaws")
      )
   }

   @Test
   fun `can call extension function against named parameter`():Unit = runBlocking {
      val (vyne, stub) = testVyne(
         """
            model Movie {
               cast : Actor[]
            }
            model Actor {
               name : Name inherits String
            }
            service Movies {
               operation findOne():Movie
            }
         """
      )
      stub.addResponse("findOne", """{"cast" : [{"name": "Mark"}, {"name" : "Carrie" }]}""")
      val result = vyne.query("""
            find { Movie } as (cast:Actor[]) -> {
               starring : Actor[] by cast.filter( (Name) -> Name == 'Mark' )
            }
      """)
         .firstRawObject()
      result.shouldBe(mapOf("starring" to listOf(mapOf("name" to "Mark"))))
   }

}
