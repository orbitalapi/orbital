package com.orbitalhq

import com.orbitalhq.models.TypedInstance
import com.orbitalhq.models.json.addJson
import com.orbitalhq.models.json.addKeyValuePair
import com.orbitalhq.query.DirectServiceInvocationStrategy
import com.orbitalhq.query.QueryEngineFactory
import com.orbitalhq.schemas.QueryOptions
import io.kotest.common.runBlocking
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class GraphPlannerMultiplePathsTest {

   /**
    * In this test, there are two facts made available - both have paths to load data.
    * The path from the first fact is longer than the path from the second fact.
    * The engine should use the path from the second fact
    */
   @Test
   fun `when scalar facts provide multiple paths then shortest is chosen`():Unit = runBlocking {
      val (vyne, stub) = testVyne(
         """
         model Film {
            id : FilmId inherits Int
            earningsReport: RevenueReportId
         }
         model Studio {
            id : StudioId inherits Int
            filmId : FilmId
         }
         model Revenue {
            id : RevenueReportId inherits Int
            earnings : Earnings inherits Decimal
         }
         service FilmsApi {
            operation getStudio(StudioId):Studio
            operation getFilm(FilmId):Film
            operation getRevenue(RevenueReportId):Revenue
         }
      """.trimIndent(),
         // When running this test, remove the DirectServiceInvocationStrategy,
         // which forces operation invocation through the graph search
         queryStrategyFilter = QueryEngineFactory.excludeQueryStrategies(listOf(DirectServiceInvocationStrategy::class))
      )

      // Add facts into the search context.
      // Specifically adding them "worst first", so that if we
      // iterate these facts, we'll pick a poor-er performing search (more hops).
      vyne.addKeyValuePair("StudioId", 123)
      vyne.addKeyValuePair("FilmId", 456)
      vyne.addKeyValuePair("RevenueReportId", 789)

      stub.addResponse("getStudio", """{ "id" : 123,  "filmId" : 456 }""")
      stub.addResponse("getFilm", """{ "id" : 456,  "earningsReport" : 789 }""")
      stub.addResponse("getRevenue", """{ "id" : 789,  "earnings" : 100.00 }""")

      vyne.query("""find { Revenue }""")
         .firstRawObject()
         .shouldBe(mapOf("id" to 789, "earnings" to 100.00.toBigDecimal()))

      // Verify that we didn't call unnecessary services.
      stub.calls["getStudio"].shouldBeEmpty()
      stub.calls["getFilm"].shouldBeEmpty()

   }

   @Test
   fun `when object facts provide multiple paths then shortest is chosen`():Unit = runBlocking {
      val (vyne, stub) = testVyne(
         """
         model Film {
            id : FilmId inherits Int
            earningsReport: RevenueReportId
         }
         model Studio {
            id : StudioId inherits Int
            filmId : FilmId
         }
         model Revenue {
            id : RevenueReportId inherits Int
            earnings : Earnings inherits Decimal
         }
         service FilmsApi {
            operation getStudio(StudioId):Studio
            operation getFilm(FilmId):Film
            operation getRevenue(RevenueReportId):Revenue
         }
      """.trimIndent(),
         // When running this test, remove the DirectServiceInvocationStrategy,
         // which forces operation invocation through the graph search
         queryStrategyFilter = QueryEngineFactory.excludeQueryStrategies(listOf(DirectServiceInvocationStrategy::class))
      )

      // Add facts into the search context.
      // Specifically adding them "worst first", so that if we
      // iterate these facts, we'll pick a poor-er performing search (more hops).
      vyne.addKeyValuePair("StudioId", 123)
      vyne.addJson("Film","""{ "id" : 456,  "earningsReport" : 789 }""")

      stub.addResponse("getStudio", """{ "id" : 123,  "filmId" : 456 }""")
      stub.addResponse("getFilm", """{ "id" : 456,  "earningsReport" : 789 }""")
      stub.addResponse("getRevenue", """{ "id" : 789,  "earnings" : 100.00 }""")

      vyne.query("""find { Revenue }""")
         .firstRawObject()
         .shouldBe(mapOf("id" to 789, "earnings" to 100.00.toBigDecimal()))

      // Verify that we didn't call unnecessary services.
      stub.calls["getStudio"].shouldBeEmpty()
      stub.calls["getFilm"].shouldBeEmpty()
   }

   @Test
   fun `when object facts provide multiple paths and one uses supertype for id then shortest is chosen`():Unit = runBlocking {
      val (vyne, stub) = testVyne(
         """
         type RevenueReportId inherits Int
         model Film {
            id : FilmId inherits Int
            // Note that the attribute is using a subtype
            earningsReport: FilmEarningsReportId inherits RevenueReportId
         }
         model Studio {
            id : StudioId inherits Int
            filmId : FilmId
         }
         model Revenue {
            id : RevenueReportId
            earnings : Earnings inherits Decimal
         }
         service FilmsApi {
            operation getStudio(StudioId):Studio
            operation getFilm(FilmId):Film
            operation getRevenue(RevenueReportId):Revenue
         }
      """.trimIndent(),
         // When running this test, remove the DirectServiceInvocationStrategy,
         // which forces operation invocation through the graph search
         queryStrategyFilter = QueryEngineFactory.excludeQueryStrategies(listOf(DirectServiceInvocationStrategy::class))
      )

      // Add facts into the search context.
      // Specifically adding them "worst first", so that if we
      // iterate these facts, we'll pick a poor-er performing search (more hops).
      vyne.addKeyValuePair("StudioId", 123)
      vyne.addJson("Film","""{ "id" : 456,  "earningsReport" : 789 }""")

      stub.addResponse("getStudio", """{ "id" : 123,  "filmId" : 456 }""")
      stub.addResponse("getFilm", """{ "id" : 456,  "earningsReport" : 789 }""")
      stub.addResponse("getRevenue", """{ "id" : 789,  "earnings" : 100.00 }""")

      vyne.query("""find { Revenue }""")
         .firstRawObject()
         .shouldBe(mapOf("id" to 789, "earnings" to 100.00.toBigDecimal()))

      // Verify that we didn't call unnecessary services.
      stub.calls["getStudio"].shouldBeEmpty()
      stub.calls["getFilm"].shouldBeEmpty()
   }
}
