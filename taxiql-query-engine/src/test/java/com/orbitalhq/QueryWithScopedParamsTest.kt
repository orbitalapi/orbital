package com.orbitalhq

import com.orbitalhq.models.json.parseJson
import io.kotest.common.runBlocking
import io.kotest.matchers.shouldBe
import org.junit.Test
import kotlin.test.assertFailsWith

class QueryWithScopedParamsTest {

   @Test
   fun `can pass a scoped param to Vyne`(): Unit = runBlocking {
      val (vyne, stub) = testVyne(
         """
         model Film {
            filmId : FilmId inherits Int
         }
         service Films {
            operation findFilm(FilmId):Film(...)
         }
      """.trimIndent()
      )
      stub.addResponse("findFilm", vyne.parseJson("Film", """{ "filmId" : 123 }"""))
      val result = vyne.query(
         """query FindFilm( filmId : FilmId, wrongFilmId : FilmId) {
   find { Film( FilmId == filmId ) }
}""", arguments = mapOf("wrongFilmId" to 456, "filmId" to 123)
      ).firstRawObject()

      result.shouldBe(mapOf("filmId" to 123))
      // Make sure the correct arg was passed to the service
      stub.invocations["findFilm"]!!.first().value.shouldBe(123)
   }

   @Test
   fun `fails if argument is not provided`():Unit = runBlocking {
      val (vyne, stub) = testVyne(
         """
         model Film {
            filmId : FilmId inherits Int
         }
         service Films {
            operation findFilm(FilmId):Film
         }
      """.trimIndent()
      )
      assertFailsWith<IllegalStateException>("No value was provided for parameter filmId") {
         val result = vyne.query(
            """query FindFilm( filmId : FilmId ) {
   find { Film( FilmId == filmId ) }
}""", arguments = mapOf("wrongFilmId" to 456)
         ).firstRawObject()
      }
   }


   @Test
   fun `can evaluate expression in given clause`():Unit = runBlocking {
      val (vyne) = testVyne("")
       vyne.query("""given { message : String = upperCase("hello") }
          |find { String }
       """.trimMargin())
          .firstRawValue()
          .shouldBe("HELLO")
   }

   @Test
   fun `can evaluate expression that references constant in given clause`():Unit = runBlocking {
      val (vyne) = testVyne("type Result inherits String")
      vyne.query("""given { message : String = "hello", result : Result = upperCase(message) }
          |find { Result }
       """.trimMargin())
         .firstRawValue()
         .shouldBe("HELLO")
   }

   @Test
   fun `can evaluate expression that references another expression in given clause`():Unit = runBlocking {
      val (vyne) = testVyne("type Result inherits String")
      vyne.query("""given { message : String = "Hello", upper : String = upperCase(message), result : Result = lowerCase(message) }
          |find { Result }
       """.trimMargin())
         .firstRawValue()
         .shouldBe("hello")
   }

   @Test
   fun `can use a cast expression against a parameter in a given clause`(): Unit = runBlocking {
      val (vyne) = testVyne("""
         type PersonId inherits String
         type HumanId inherits String
      """.trimIndent())
      vyne.query("""
         query findPerson(personId : PersonId) {
            given { humanId : HumanId = (HumanId) personId }
            find { human : HumanId }
         }
      """.trimIndent(), arguments = mapOf("personId" to "123")
      )
         .firstRawObject()
         .shouldBe(mapOf("human" to "123"))
   }

   @Test
   fun `can use a cast expression in a given clause`():Unit = runBlocking {
      val (vyne) = testVyne("""
         type PersonId inherits String
         type HumanId inherits String
      """.trimIndent())
      vyne.query("""
            given { humanId : HumanId = (HumanId) "123" }
            find { human : HumanId }
      """.trimIndent()
      )
         .firstRawObject()
         .shouldBe(mapOf("human" to "123"))
   }

   @Test
   fun `can use a variable from a saved query in projection type expression`():Unit = runBlocking {
      val (vyne,stub) = testVyne("""
            model Film {
               title : Title inherits String
               cast : Actor[]
            }
            model Actor {
               name : PersonName inherits String
            }
            service Films {
               operation getFilms():Film[]
            }
""")
      val film = vyne.parseJson(
         "Film[]",
         """[{ "title" :"Star Wars" , "cast" : [ {"name" : "Mark"}, {"name" : "Carrie"}, {"name": "Harrison"} ] }]"""
      )
      stub.addResponse("getFilms", film)
         val queryResult = vyne.query("""
               query FindSomeFilms( starring : PersonName ) {
      find { Film[] } as (filter(Actor[], (PersonName) -> PersonName == starring)) -> {
         name: PersonName
      }[]
   }
         """.trimIndent(), arguments = mapOf("starring" to "Mark")
         )
            .firstRawObject()
      // Note - the value we projected was the filtered actors, not the film.
      queryResult.shouldBe(mapOf("name" to "Mark"))
   }
   @Test
   fun `can use a variable from a saved query in a field projection`():Unit = runBlocking {
      val (vyne,stub) = testVyne("""
            model Film {
               title : Title inherits String
               cast : Actor[]
            }
            model Actor {
               name : PersonName inherits String
            }
            service Films {
               operation getFilms():Film[]
            }
""")
      val film = vyne.parseJson(
         "Film[]",
         """[{ "title" :"Star Wars" , "cast" : [ {"name" : "Mark"}, {"name" : "Carrie"}, {"name": "Harrison"} ] }]"""
      )
      stub.addResponse("getFilms", film)
      val queryResult = vyne.query("""
      query FindSomeFilms( starring : PersonName ) {
         find { Film[] } as  {
            title: Title
            starring: filter(Actor[], (PersonName) -> PersonName == starring) as {
              name : PersonName
           }[]
         }[]
      }
         """.trimIndent(), arguments = mapOf("starring" to "Mark")
      )
         .firstRawObject()
      queryResult.shouldBe(mapOf(
         "title" to "Star Wars",
         "starring" to
            listOf(
               mapOf("name" to "Mark")
            )

         ))
   }
   @Test
   fun `can use a variable by name in a projection scope in body`():Unit = runBlocking {
      val (vyne,stub) = testVyne("""
            model Film {
               title : Title inherits String
               cast : Actor[]
            }
            model Actor {
               name : PersonName inherits String
            }
            service Films {
               operation getFilm():Film
            }
""")
      stub.addResponse("getFilm", """{ "title" :"Star Wars" , "cast" : [ {"name" : "Mark"}, {"name" : "Carrie"}, {"name": "Harrison"} ] }""")
      val result = vyne.query("""
         find { Film } as (starring: Actor[].first()) -> {
            leadActor: starring
         }
      """.trimIndent())
         .firstRawObject()
      result.shouldBe(mapOf("leadActor" to mapOf("name" to "Mark")))
   }

   @Test
   fun `can use a variable by type in a projection scope in body`():Unit = runBlocking {
      val (vyne,stub) = testVyne("""
            model Film {
               title : Title inherits String
               cast : Actor[]
            }
            model Actor {
               name : PersonName inherits String
            }
            service Films {
               operation getFilm():Film
            }
""")
      stub.addResponse("getFilm", """{ "title" :"Star Wars" , "cast" : [ {"name" : "Mark"}, {"name" : "Carrie"}, {"name": "Harrison"} ] }""")
      val result = vyne.query("""
         find { Film } as (starring: Actor = Actor[].first()) -> {
            leadActor: Actor
         }
      """.trimIndent())
         .firstRawObject()
      result.shouldBe(mapOf("leadActor" to mapOf("name" to "Mark")))
   }

   @Test
   fun `can use a variable by type in a projection scope in body without explicit type in find clause and using type traversal`():Unit = runBlocking {
      val (vyne,stub) = testVyne("""
            model Film {
               title : Title inherits String
               cast : Actor[]
            }
            model Actor {
               name : PersonName inherits String
            }
            service Films {
               operation getFilm():Film
            }
""")
      stub.addResponse("getFilm", """{ "title" :"Star Wars" , "cast" : [ {"name" : "Mark"}, {"name" : "Carrie"}, {"name": "Harrison"} ] }""")
      val result = vyne.query("""
         find { "" } as (starring: Actor = Film::Actor[].first()) -> {
            leadActor: Actor
         }
      """.trimIndent())
         .firstRawObject()
      result.shouldBe(mapOf("leadActor" to mapOf("name" to "Mark")))
   }
   @Test
   fun `can use a variable by type in a projection scope in body without explicit type in find clause using property traversal`():Unit = runBlocking {
      val (vyne,stub) = testVyne("""
            model Film {
               title : Title inherits String
               cast : Actor[]
            }
            model Actor {
               name : PersonName inherits String
            }
            service Films {
               operation getFilm():Film
            }
""")
      stub.addResponse("getFilm", """{ "title" :"Star Wars" , "cast" : [ {"name" : "Mark"}, {"name" : "Carrie"}, {"name": "Harrison"} ] }""")
      val result = vyne.query("""
         find { "" } as (film: Film, starring: Actor = film.cast.first()) -> {
            leadActor: Actor
         }
      """.trimIndent())
         .firstRawObject()
      result.shouldBe(mapOf("leadActor" to mapOf("name" to "Mark")))
   }
}
