package com.orbitalhq

import com.orbitalhq.models.TypedCollection
import com.orbitalhq.models.json.parseJson
import com.orbitalhq.query.UnresolvedTypeInQueryException
import com.orbitalhq.query.graph.operationInvocation.SearchRuntimeException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.jupiter.api.assertThrows

class MutationQueryTest {

   @Test
   fun `a mutating operation can accept an array 1`(): Unit = runBlocking {
      val (vyne, stub) = testVyne(
         """
         model Film {
            title : Title inherits String
            yearReleased : YearReleased inherits Int
         }
         parameter model Movie {
            name : Title
            year : YearReleased
         }

         model FilmCatalog {
            films : Film[]
         }

         service Films {
            operation getFilms():FilmCatalog
            write operation saveMovies(Movie[]):Movie[]
         }
      """.trimIndent()
      )


      stub.addResponse(
         "getFilms", vyne.parseJson(
            "FilmCatalog", """
         { "films" : [ { "title" : "Star Wars", "yearReleased" : 1978 },  { "title" : "Empire Strikes Back", "yearReleased" : 1982 }] }
      """.trimIndent()
         )
      )
      stub.addResponseReturningInputs("saveMovies")

      val results = vyne.query("find { FilmCatalog } as (films:Film[]) -> Movie[] call Films::saveMovies")
         .rawObjects()

      val calls = stub.calls["saveMovies"]
      calls.shouldHaveSize(1)
      val mutationCall = calls.single()
      val input = mutationCall[0]
      input.shouldBeInstanceOf<TypedCollection>()
         .shouldHaveSize(2)
   }


   @Test
   fun `a mutating operation can accept an array`(): Unit = runBlocking {
      val (vyne, stub) = testVyne(
         """
         model Film {
            title : Title inherits String
            yearReleased : YearReleased inherits Int
         }
         parameter model Movie {
            name : Title
            year : YearReleased
         }
         service Films {
            operation getFilms():Film[]
            write operation saveMovies(Movie[]):Movie[]
         }
      """.trimIndent()
      )
      stub.addResponse(
         "getFilms", vyne.parseJson(
            "Film[]",
            """[ { "title" : "Star Wars", "yearReleased" : 1978 },
            | { "title" : "Empire Strikes Back", "yearReleased" : 1982 }
            | ]""".trimMargin()
         )
      )
      stub.addResponseReturningInputs("saveMovies")

      val results = vyne.query("find { Film[] } call Films::saveMovies")
         .rawObjects()

      val calls = stub.calls["saveMovies"]
      calls.shouldHaveSize(1)
      val mutationCall = calls.single()
      val input = mutationCall[0]
      input.shouldBeInstanceOf<TypedCollection>()
         .shouldHaveSize(2)
   }

   @Test
   fun `a mutating operation with explicit projection can accept an array`(): Unit = runBlocking {
      val (vyne, stub) = testVyne(
         """
         model Film {
            title : Title inherits String
            yearReleased : YearReleased inherits Int
         }
         parameter model Movie {
            name : Title
            year : YearReleased
         }
         service Films {
            operation getFilms():Film[]
            write operation saveMovies(Movie[]):Movie[]
         }
      """.trimIndent()
      )
      stub.addResponse(
         "getFilms", vyne.parseJson(
            "Film[]",
            """[ { "title" : "Star Wars", "yearReleased" : 1978 },
            | { "title" : "Empire Strikes Back", "yearReleased" : 1982 }
            | ]""".trimMargin()
         )
      )
      stub.addResponseReturningInputs("saveMovies")

      val results = vyne.query("find { Film[] } as Movie[] call Films::saveMovies")
         .rawObjects()

      val calls = stub.calls["saveMovies"]
      calls.shouldHaveSize(1)
      val mutationCall = calls.single()
      val input = mutationCall[0]
      input.shouldBeInstanceOf<TypedCollection>()
         .shouldHaveSize(2)
   }

   /**
    * For now, we can't support this, as we'd just collect forever.
    * In future, we can support some form of checkpointing or windowing.
    * So, for now, this should error
    */
   @Test
   fun `a mutating operation accepting an array should throw an error with a streaming source`(): Unit = runBlocking {
      val (vyne, stub) = testVyne(
         """
         model Film {
            title : Title inherits String
            yearReleased : YearReleased inherits Int
         }
         parameter model Movie {
            name : Title
            year : YearReleased
         }
         service Films {
            operation getFilms():Stream<Film>
            write operation saveMovies(Movie[]):Movie[]
         }
      """.trimIndent()
      )
      stub.addResponse(
         "getFilms", vyne.parseJson(
            "Film[]",
            """[ { "title" : "Star Wars", "yearReleased" : 1978 },
            | { "title" : "Empire Strikes Back", "yearReleased" : 1982 }
            | ]""".trimMargin()
         )
      )
      stub.addResponseReturningInputs("saveMovies")

      shouldThrow<SearchRuntimeException> {
         // This should error, with a helpful error message
         vyne.query("stream { Film } call Films::saveMovies")
            .rawObjects()
      }
   }


   @Test
   fun `will invoke a write service for a mutation`(): Unit = runBlocking {
      val (vyne, stub) = testVyne(
         """
         model Person {
            personId: PersonId inherits String
         }

         service Peeps {
            write operation updatePerson(Person) : Person
         }
      """
      )
      stub.addResponse("updatePerson") { _, params ->
         listOf(vyne.parseJson("Person", """{ "personId" : "updated-jimmy" } """))
      }
      val result = vyne.query(
         """given { person:Person = { personId : "jimmy" } }
         call Peeps::updatePerson
      """
      )
         .firstRawObject()
      result.shouldBe(mapOf("personId" to "updated-jimmy"))
      val passedInput = stub.invocations["updatePerson"]!!.get(0)
      passedInput.toRawObject().shouldBe(mapOf("personId" to "jimmy"))
   }

   @Test
   fun `will not invoke a write service when doing a find`(): Unit = runBlocking {
      val (vyne, stub) = testVyne(
         """
         closed model Person {
            personId: PersonId inherits String
         }

         service Peeps {
            write operation updatePerson(PersonId) : Person
         }
      """
      )
      stub.addResponse("updatePerson") { _, params ->
         listOf(vyne.parseJson("Person", """{ "personId" : "updated-jimmy" } """))
      }
      assertThrows<UnresolvedTypeInQueryException> {
         vyne.query(
            """given { person:PersonId =  "jimmy" }
         find { Person }
      """.trimMargin()
         ).typedObjects()
      }
   }

   @Test
   fun `in a query that finds then mutates the writing operation is only invoked during the mutation`(): Unit =
      runBlocking {
         val (vyne, stub) = testVyne(
            """
         model Person {
            personId: PersonId inherits String
            anotherId: AnotherId inherits String
         }

         service Peeps {
            write operation deletePerson(AnotherId) : Person
            operation findPerson(PersonId):Person

         }
      """
         )
         stub.addResponse("findPerson") { _, params ->
            listOf(vyne.parseJson("Person", """{ "personId" : "jimmy", "anotherId" : "found-jimmy" } """))
         }
         stub.addResponse("deletePerson") { _, params ->
            listOf(vyne.parseJson("Person", """{ "personId" : "jimmy", "anotherId" : "deleted-jimmy" } """))
         }
         val result = vyne.query(
            """given { person:PersonId =  "jimmy" }
         find { Person }  // First find,
         call Peeps::deletePerson // then delete
      """.trimMargin()
         )
            .firstRawObject()
         // The result should be from our deletePerson service
         result.shouldBe(mapOf("personId" to "jimmy", "anotherId" to "deleted-jimmy"))

         // When we called findPerson, it should be with the original id
         stub.invocations["findPerson"]!!.single().toRawObject().shouldBe("jimmy")
         // This should be with the value from our discovered service
         stub.invocations["deletePerson"]!!.single().toRawObject().shouldBe("found-jimmy")
      }

   @Test
   fun `a parameter defined in a given clause can be passed to a mutation operation`(): Unit = runBlocking {
      val (vyne, stub) = testVyne(
         """
         type AuthKey inherits String
         model Person {
            personId: PersonId inherits String
            anotherId: AnotherId inherits String
         }

         service Peeps {
            write operation deletePerson(AnotherId, authKey: AuthKey) : Person
            operation findPerson(PersonId):Person

         }
      """
      )
      val person = vyne.parseJson("Person", """{ "personId" : "jimmy", "anotherId" : "found-jimmy" } """)
      stub.addResponse("findPerson") { _, _ -> listOf(person) }
      stub.addResponse("deletePerson") { _, _ -> listOf(person) }
      val result = vyne.query(
         """given { person:PersonId =  "jimmy", authKey:AuthKey = "foo" }
         find { Person }  // First find,
         call Peeps::deletePerson // then delete
      """.trimMargin()
      )
         .firstRawObject()
      val params = stub.calls["deletePerson"].single()
      params[1].value.shouldBe("foo")
   }

   @Test
   fun `a mutating query containing a projection with a cast expression passes variables properly`(): Unit =
      runBlocking {
         val (vyne, stub) = testVyne(
            """
         closed model Film {
            filmId : FilmId inherits String
            title : Title inherits String
         }
         closed parameter model Movie {
            id : MovieId inherits Int
            name : Title
          }
          service FilmsApi {
            operation getFilms():Film[]
            write operation saveMovie(Movie):Movie
         }
      """.trimIndent()
         )
         stub.addResponseReturningInputs("saveMovie")
         stub.addResponse("getFilms", vyne.parseJson("Film[]", """[ { "filmId" : "1" , "title" : "Jaws" } ]"""))
         vyne.query(
            """
         find { Film[] } as {
            id : MovieId = (MovieId) FilmId
            name : Title
         }[]
         call FilmsApi::saveMovie
        """.trimIndent()
         )
            .firstRawObject()
         val callInputs = stub.calls["saveMovie"].first()
         val inputObject = callInputs.single().toRawObject()
         inputObject.shouldBe(
            mapOf(
               "id" to 1, // note that this isn't "1"
               "name" to "Jaws"
            )
         )
      }

   @Test
   fun `can use a default param in a mutation`(): Unit = runBlocking {
      val (vyne, stub) = testVyne(
         """
         type AuthKey inherits String
         model Person {
            personId: PersonId inherits String
            anotherId: AnotherId inherits String
         }

         service Peeps {
            write operation deletePerson(AnotherId, authKey: AuthKey = "foo") : Person
            operation findPerson(PersonId):Person

         }
      """
      )
      val person = vyne.parseJson("Person", """{ "personId" : "jimmy", "anotherId" : "found-jimmy" } """)
      stub.addResponse("findPerson") { _, _ -> listOf(person) }
      stub.addResponse("deletePerson") { _, _ -> listOf(person) }
      val result = vyne.query(
         """find { Person }  // First find,
         call Peeps::deletePerson // then delete
      """.trimMargin()
      )
         .firstRawObject()
      val params = stub.calls["deletePerson"].single()
      params[1].value.shouldBe("foo")
   }

   @Test
   fun `streaming source with spread operator projection into mutating target`():Unit = runBlocking {
      val (vyne,stub) = testVyne(
         """
            model StockRating {
               creditRating : CreditRating inherits String
            }
            model StockQuote {
               ticker : Ticker inherits String
               price : Price inherits Decimal
               quantity : Quantity inherits Int
            }
            parameter model SavedQuote {
               ticker : Ticker
               price : Price
               quantity : Quantity
               message : Message inherits String
               rating : StockRating[]
            }
            service StockApi {
               stream quotes : Stream<StockQuote>
               write operation saveQuote(SavedQuote):SavedQuote
               operation getRating(Ticker):StockRating
            }
         """.trimIndent()
      )
      stub.addResponseReturningInputs("saveQuote")
      stub.addResponse("getRating", """{ "creditRating" : "AAA" }""")
      stub.addResponseFlow("quotes") { _,_, ->
         flowOf(
            vyne.parseJson("StockQuote", """{ "ticker" : "AAPL", "price" : 234.56, "quantity" : 100000 }""")
         )
      }

      val result = vyne.query("""stream { StockQuote } as {
         |  message : Message = 'Hello, world'
         |  rating : StockRating[] = listOf(StockRating)
         |  ...
         |}[]
         |call StockApi::saveQuote
      """.trimMargin())
         .firstRawObject()
      result.shouldBe(mapOf(
         "ticker" to "AAPL",
         "price" to 234.56.toBigDecimal(),
         "quantity" to 100000,
         "message" to "Hello, world",
         "rating" to listOf(
            mapOf("creditRating" to "AAA")
         )
      ))
   }
}
