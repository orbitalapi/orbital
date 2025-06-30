package com.orbitalhq

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.runBlocking
import org.junit.Ignore
import org.junit.Test
import org.junit.jupiter.api.Disabled

class ChainedProjectionsTest {
   @Test
   fun `can chain multiple projections`(): Unit = runBlocking {
      val (vyne, stub) = testVyne(
         """
         model Film {
            id : FilmId inherits Int
         }

         model FilmSales {
            salesForecastId : SalesForecastId inherits Int
         }
         model SalesForecast {
            projectedProfit: ProjectedProfit inherits Decimal
         }

         service FilmsApi {
            operation getFilms():Film
            operation getFilmSales(FilmId):FilmSales
            operation getSalesForecast(FilmId):SalesForecast
         }
      """.trimIndent()
      )
      stub.addResponse("getFilms", """{ "id" : 123}""")
      stub.addResponse("getFilmSales", """{ "salesForecastId" : 456 }""")
      stub.addResponse("getSalesForecast", """{ "projectedProfit" : 1000.00 }""")

      val result = vyne.query(
         """
         find { Film } as {
            id : FilmId
            // Note: The nested projections here aren't strictly
            // necessary, but they're here to test the capability
            projectedProfit : FilmSales as SalesForecast as ProjectedProfit
         }
      """.trimIndent()
      )
         .firstRawObject()

      result.shouldBe(mapOf("id" to 123, "projectedProfit" to 1000.00.toBigDecimal()))
   }

   @Test
   fun `can chain projections to access iteration scope`(): Unit = runBlocking {
      val (vyne, stub) = testVyne(
         """
         model Film {
            id : FilmId inherits Int
         }

         model Actor {
            name : PersonName inherits String
         }
         model CastResponse {
            actors : Actor[]
         }
         service FilmsApi {
            operation getFilms():Film
            operation getCast(FilmId):CastResponse
         }
      """.trimIndent()
      )
      stub.addResponse("getFilms", """{ "id" : 123}""")
      stub.addResponse("getCast", """{ "actors" : [ { "name" : "Jimmy" } ] }""")

      val result = vyne.query(
         """
         find { Film } as {
            id : FilmId
            cast : CastResponse as Actor[] as (actor:Actor) -> {
               personName : PersonName
            }[]
         }
      """.trimIndent()
      )
         .firstRawObject()

      result.shouldBe(mapOf("id" to 123, "cast" to listOf(mapOf("personName" to "Jimmy"))))
   }

   @Test
   fun `can chain projections in nested anonymous object to access iteration scope`(): Unit = runBlocking {
      val (vyne, stub) = testVyne(
         """
         model Film {
            id : FilmId inherits Int
         }

         model Studio {
            id : StudioId inherits Int
         }

         model Actor {
            name : PersonName inherits String
         }
         model CastResponse {
            actors : Actor[]
         }
         service FilmsApi {
            operation getStudio():Studio
            operation getFilms(StudioId):Film
            operation getCast(FilmId):CastResponse
         }
      """.trimIndent()
      )
      stub.addResponse("getStudio", """{ "id" : 456 }""")
      stub.addResponse("getFilms", """{ "id" : 123 }""")
      stub.addResponse("getCast", """{ "actors" : [ { "name" : "Jimmy" }, { "name" : "Jane" } ] }""")

      val result = vyne.query(
         """
         find { Studio } as {
            studioId : StudioId
            film :  Film as {
               id : FilmId
               cast : CastResponse as Actor[] as (actor:Actor) -> {
                  personName : PersonName
               }[]
            }
         }
      """.trimIndent()
      )
         .firstRawObject()

      result.shouldBe(
         mapOf(
            "studioId" to 456,
            "film" to
               mapOf(
                  "id" to 123, "cast" to listOf(
                     mapOf("personName" to "Jimmy"),
                     mapOf("personName" to "Jane"),
                  )
               )
         )
      )
   }

   @Test
   fun `can chain projections in nested anonymous object with inherited id type to access iteration scope`(): Unit =
      runBlocking {
         // This test covers using a graph search to invoke a service,
         // where input parameter is a supertype of a known fact,
         // and that fact is coming within a nested scoped projection.
         // The root cause was not calling .forTypeAndSuperTypes() when adding
         // a fact into the graph.
         // However, it's not 100% clear why this appeared in this test, where we have other
         // tests that cover this behaviour.
         val (vyne, stub) = testVyne(
            """
         type StudioProductionId inherits Int
         model Film {
            id : FilmId inherits StudioProductionId
         }

         model Studio {
            id : StudioId inherits Int
         }

         model Actor {
            name : PersonName inherits String
         }
         model CastResponse {
            actors : Actor[]
         }
         service FilmsApi {
            operation getStudio():Studio
            operation getFilms(StudioId):Film
            operation getCast(StudioProductionId):CastResponse
         }
      """.trimIndent()
         )
         stub.addResponse("getStudio", """{ "id" : 456 }""")
         stub.addResponse("getFilms", """{ "id" : 123 }""")
         stub.addResponse("getCast", """{ "actors" : [ { "name" : "Jimmy" }, { "name" : "Jane" } ] }""")

         val result = vyne.query(
            """
         find { Studio } as {
            studioId : StudioId
            film :  Film as {
               id : FilmId
               cast : CastResponse as Actor[] as (actor:Actor) -> {
                  personName : PersonName
               }[]
            }
         }
      """.trimIndent()
         )
            .firstRawObject()

         result.shouldBe(
            mapOf(
               "studioId" to 456,
               "film" to
                  mapOf(
                     "id" to 123, "cast" to listOf(
                        mapOf("personName" to "Jimmy"),
                        mapOf("personName" to "Jane"),
                     )
                  )
            )
         )
      }

   @Test
   fun `can chain projections in nested anonymous object with array property and inherited id type to access iteration scope`(): Unit =
      runBlocking {
         // This test covers behaviour found when a nested projection needs to reference a scoped
         // variable for a search.
         // eg:
         // find { Studio } as { <--- one scope
         //   studioId : StudioId
         //   film :  FilmResponse as (first(Film[])) -> {  <----- 1st nested scope
         //      id : FilmId
         //      cast : CastResponse as Actor[]  as (actor:Actor) -> { //<---- discovery needs value from nested scope
         //         personName : PersonName
         //      }[]
         //   }
         //}
         //
         // The issue was search context was not correctly being propogated, so values from the 1st
         // nested scope were not passed down into the search for the discovery type, meaning searches failed.
         // This strictly isn't related to chained projections, but this is where we found it.

         val (vyne, stub) = testVyne(
            """
         type StudioProductionId inherits Int
         model FilmResponse {
            films : Film[]
         }
         model Film {
            id : FilmId inherits StudioProductionId
         }

         model Studio {
            id : StudioId inherits Int
         }

         model Actor {
            name : PersonName inherits String
         }
         model CastResponse {
            actors : Actor[]
         }
         service FilmsApi {
            operation getStudio():Studio
            operation getFilms(StudioId):FilmResponse
            operation getCast(StudioProductionId):CastResponse
         }
      """.trimIndent()
         )
         stub.addResponse("getStudio", """{ "id" : 456 }""")
         stub.addResponse("getFilms", """{ "films" : [{ "id" : 123 }] }""")
         stub.addResponse("getCast", """{ "actors" : [ { "name" : "Jimmy" }, { "name" : "Jane" } ] }""")

         val result = vyne.query(
            """
         find { Studio } as {
            studioId : StudioId
            film :  FilmResponse as (first(Film[])) -> {
               id : FilmId
               cast : CastResponse as Actor[]  as (actor:Actor) -> {
                  personName : PersonName
               }[]
            }
         }
      """.trimIndent()
         )
            .firstRawObject()

         result.shouldBe(
            mapOf(
               "studioId" to 456,
               "film" to
                  mapOf(
                     "id" to 123, "cast" to listOf(
                        mapOf("personName" to "Jimmy"),
                        mapOf("personName" to "Jane"),
                     )
                  )
            )
         )
      }


   @Test
   fun `field projection no chaining`(): Unit = runBlocking {
      val (vyne, stub) = testVyne(
         """
         model Film {
            id : FilmId inherits Int
         }

         model Actor {
            name : PersonName inherits String
         }
         model CastResponse {
            actors : Actor[]
         }
         service FilmsApi {
            operation getFilms():Film
            operation getCast(FilmId):CastResponse
         }
      """.trimIndent()
      )
      stub.addResponse("getFilms", """{ "id" : 123}""")
      stub.addResponse("getCast", """{ "actors" : [ { "name" : "Jimmy" } ] }""")

      val result = vyne.query(
         """
         find { Film } as {
            id : FilmId
            castResponse : CastResponse as {
               cast: Actor[] as {
                  actorName : PersonName
               }[]
            }
         }
      """.trimIndent()
      )
         .firstRawObject()

      result.shouldBe(mapOf("id" to 123, "castResponse" to mapOf("cast" to listOf(mapOf("actorName" to "Jimmy")))))
   }

   @Test
   fun `can chain top-level projections to access iteration scope`(): Unit = runBlocking {
      val (vyne, stub) = testVyne(
         """
         model Film {
            id : FilmId inherits Int
         }

         model Actor {
            name : PersonName inherits String
         }
         model CastResponse {
            actors : Actor[]
         }
         service FilmsApi {
            operation getFilms():Film
            operation getCast(FilmId):CastResponse
         }
      """.trimIndent()
      )
      stub.addResponse("getFilms", """{ "id" : 123}""")
      stub.addResponse("getCast", """{ "actors" : [ { "name" : "Jimmy" } ] }""")

      val result = vyne.query(
         """
         find { Film } as CastResponse as Actor[] as (actor:Actor) -> {
               personName : PersonName
            }[]
      """.trimIndent()
      )
         .rawObjects()


      result.shouldBe(listOf(mapOf("personName" to "Jimmy")))
   }

   @Test
   fun `can use top-level projections which aren't chained`(): Unit = runBlocking {
      val (vyne, stub) = testVyne(
         """
         model Film {
            id : FilmId inherits Int
         }

         model Actor {
            name : PersonName inherits String
         }
         model CastResponse {
            actors : Actor[]
         }
         service FilmsApi {
            operation getFilms():Film
            operation getCast(FilmId):CastResponse
         }
      """.trimIndent()
      )
      stub.addResponse("getFilms", """{ "id" : 123}""")
      stub.addResponse("getCast", """{ "actors" : [ { "name" : "Jimmy" } ] }""")

      val result = vyne.query(
         """
         find { Film } as CastResponse
      """.trimIndent()
      )
         .firstRawObject()

      result.shouldBe(mapOf("actors" to listOf(mapOf("name" to "Jimmy"))))
   }

   @Test
   fun `can use scoped variable to supply array for iterating projection`(): Unit = runBlocking {
      val (vyne, stub) = testVyne(
         """
         model Film {
            id : FilmId inherits Int
         }

         model Actor {
            name : PersonName inherits String
         }
         model CastResponse {
            actors : Actor[]
         }
         service FilmsApi {
            operation getFilms():Film
            operation getCast(FilmId):CastResponse
         }
      """.trimIndent()
      )
      stub.addResponse("getFilms", """{ "id" : 123}""")
      stub.addResponse("getCast", """{ "actors" : [ { "name" : "Jimmy" } ] }""")

      // Just leaving this here as a reminder of why we need to support
      // converting an Object to an array -- a type-checker rule that was just relaxed.
      // This is clearly valid.
      // However, the below test case shows another scneario that makes you think
      // Hmmm.... that can't be right - we should disallow it.
      val result1 = vyne.query(
         """
         find { Film } as {
            cast : CastResponse as Actor[]
         }
      """.trimIndent()
      )
         .firstRawObject()

      result1.shouldBe(mapOf("cast" to listOf(mapOf("name" to "Jimmy"))))


      val result = vyne.query(
         """
         find { Film } as {
            id : FilmId
            // This is legal, but non-sensical.
            // It's changing the scope to include an array.
            // However, the object being projected is a CastResponse
            // (That's really important ... it's NOT an Actor[] -- that's just what's in scope).
            // This difference matters, because source object (in this case, CastResponse)
            // defines the projection behaviour - this is Object-to-array.
            // The change in scope (Actor[]) DOES NOT change the projection behaviour.
            // That's by design -- don't change this behaviour without careful thought.
            cast : CastResponse as (Actor[]) -> {
               actors: Actor[]
            }[]
         }
      """.trimIndent()
      )
         .firstRawObject()

      // MP: 30-Jun-25:
      // This used to say:
      // cast is an empty map, as not really sure what else it could be.
      // Now, it's
      // cast is an empty list
      // We can't return an empty object, when the requested type is T[]
      result.shouldBe(mapOf("id" to 123, "cast" to emptyList<Map<String,Any>>()))
   }

   @Test
   fun `projecting a value uses the input instead of another context value`(): Unit = runBlocking {
      val (vyne, stub) = testVyne(
         """
         model DealResponse {
            loanApplicationId : LoanApplicationId inherits String
            deals : Deal[]
         }
         model Deal {
            dealId : DealId inherits String
            borrowerId : BorrowerId inherits String
         }
         service DealApi {
            operation forLoanApplicationId(LoanApplicationId):DealResponse(...)
            operation forBorrowerId(BorrowerId):DealResponse(...)
         }
      """
      )
      // The two api calls return different values.
      // The point is to ensure that the values from forBorrowerId are used in the
      // correct place (as defined by the constraints in the query below)
      stub.addResponse(
         "forLoanApplicationId", """{
          "loanApplicationId" : "l1",
          "deals": [
          { "dealId" : "d1", "borrowerId" : "b1"  }
          ]}""".trimMargin()
      )
      stub.addResponse(
         "forBorrowerId", """{
          "loanApplicationId" : "l1",
          "deals": [
          { "dealId" : "d2", "borrowerId" : "b1" }, { "dealId" : "d3", "borrowerId" : "b1"  }
          ]}""".trimMargin()
      )


      val result = vyne.query(
         """
         given { loanApplicationId: LoanApplicationId = "1234" }
         find { DealResponse(LoanApplicationId == loanApplicationId) } as (deal:Deal = first(Deal[])) -> {
             id: LoanApplicationId,
             // This is the test.
             // We already have a Deal[] in scope on the parent DealResponse
             // But it's not the correct Deal[].
             // The test ensures that the result of this projection comes from
             // projecting the newly loaded DealResponse, not the existing one
             existingDeals: DealResponse(BorrowerId == deal::BorrowerId) as Deal[]
         }
      """.trimIndent()
      )
         .firstRawObject()

      result.shouldBe(
         mapOf(
            "id" to "1234",
            "existingDeals" to listOf(
               mapOf("dealId" to "d2", "borrowerId" to "b1"),
               mapOf("dealId" to "d3", "borrowerId" to "b1"),
            )
         ),
         )

      val borrowerIdCallInputs = stub.calls["forBorrowerId"]
         .shouldHaveSize(1)
         .single()
      borrowerIdCallInputs.single().toRawObject().shouldBe("b1")


   }


}
