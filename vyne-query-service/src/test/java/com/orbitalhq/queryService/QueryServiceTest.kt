package com.orbitalhq.queryService

import app.cash.turbine.testIn
import com.orbitalhq.errors.OrbitalQueryException
import com.orbitalhq.models.json.parseJsonModel
import com.orbitalhq.query.ResultMode
import com.orbitalhq.query.ValueWithTypeName
import com.orbitalhq.query.runtime.core.TEXT_CSV
import com.winterbe.expekt.should
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runBlockingTest
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.springframework.http.MediaType
import reactor.test.StepVerifier
import kotlin.test.assertEquals
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

@FlowPreview
@ExperimentalCoroutinesApi
@ExperimentalTime
class QueryServiceTest : BaseQueryServiceTest() {

   @Before
   fun setup() {
      setupTestService()
   }

   @Test
   fun `csv request produces expected results regardless of resultmode`() = runTest {
      ResultMode.values().forEach { resultMode ->
         val next = queryService.submitVyneQlQuery("""find { Order[] }""", resultMode, TEXT_CSV).block()!!.body
            .single()
         val expected = """orderId,traderName,instrumentId
orderId_0,john,Instrument_0""".trimMargin().withoutWhitespace()
         assertEquals(expected, (next as String).withoutWhitespace())
      }
   }

   @Test
   fun `csv with projection returns expected results`() = runTest {
      ResultMode.values().forEach { resultMode ->

         val result = queryService
            .submitVyneQlQuery(
               """find { Order[] } as Report[]""".trimIndent(),
               resultMode,
               TEXT_CSV
            ).block()!!.body.single()
         val expected = """orderId,tradeId,instrumentName,maturityDate,traderName
orderId_0,Trade_0,2040-11-20 0.1 Bond,2026-12-01,john
               """.withoutWhitespace()
         val item = (result as String).withoutWhitespace()
         item.should.equal(expected.withoutWhitespace())
      }

   }


   @Test
   fun `taxiQl as simple json returns expected result`() = runTest {

      val turbine = queryService.submitVyneQlQuery(
         """find { Order[] }""".trimIndent(),
         ResultMode.TYPED,
         MediaType.APPLICATION_JSON_VALUE
      ).block()!!.body.toList()

      val next = turbine.first() as ValueWithTypeName
      next.value.should.equal(
         mapOf(
            "orderId" to "orderId_0",
            "traderName" to "john",
            "instrumentId" to "Instrument_0"
         )
      )
   }

   @Test
   fun `taxiQl as raw json returns raw map`() = runTest {

      val turbine = queryService.submitVyneQlQueryStreamingResponse(
         """find { Order[] }""".trimIndent(),
         ResultMode.RAW,
         MediaType.APPLICATION_JSON_VALUE
      ).testIn(this)
      val next = turbine.awaitItem() as Map<String, Any?>
      next.should.equal(
         mapOf(
            "orderId" to "orderId_0",
            "traderName" to "john",
            "instrumentId" to "Instrument_0"
         )
      )
      turbine.awaitComplete()
   }

   @Test
   fun `taxiQl as simple json with projection returns expected result`() = runTest {

      val turbine = queryService.submitVyneQlQueryStreamingResponse(
         """find { Order[] } as Report[]""".trimIndent(),
         ResultMode.TYPED,
         MediaType.APPLICATION_JSON_VALUE
      )
         .testIn(this)
      val next = turbine.awaitItem() as ValueWithTypeName
      next.value.should.equal(
         mapOf(
            "orderId" to "orderId_0",
            "tradeId" to "Trade_0",
            "instrumentName" to "2040-11-20 0.1 Bond",
            "maturityDate" to "2026-12-01",
            "traderName" to "john"
         )
      )
      turbine.awaitComplete()
   }

   @Test
   fun `taxiQl as raw json with projection returns expected result`() = runTest {

      val turbine = queryService.submitVyneQlQueryStreamingResponse(
         """find { Order[] } as Report[]""".trimIndent(),
         ResultMode.RAW,
         MediaType.APPLICATION_JSON_VALUE
      )
         .testIn(this, timeout = Duration.INFINITE)
      val next = turbine.awaitItem() as Map<String, Any?>
      next.should.equal(
         mapOf(
            "orderId" to "orderId_0",
            "tradeId" to "Trade_0",
            "instrumentName" to "2040-11-20 0.1 Bond",
            "maturityDate" to "2026-12-01",
            "traderName" to "john"
         )
      )
      turbine.awaitComplete()
   }


   @Ignore("LENS-345 has been opened to re-enable this functionality")
   @Test
   fun `duplicate match error in projection returned as part of failed response`() = runBlockingTest {

      val maturityDateInstrument = "2025-12-01"
      stubService.addResponse(
         "getInstrument", vyne.parseJsonModel(
            "Instrument", """
            {
               "maturityDate": "$maturityDateInstrument",
               "instrumentId": "Instrument_0",
               "name": "2040-11-20 0.1 Bond"
            }
         """.trimIndent()
         )
      )

      val response = queryService.submitVyneQlQuery(
         """find { Order[] } as Report[]""".trimIndent(),
         ResultMode.TYPED,
         MediaType.APPLICATION_JSON_VALUE
      )
         .block()!!.body.toList()
      TODO("Assert the contents of the list")
//      val response = jacksonObjectMapper().readTree(responseStr)
//      response["fullyResolved"].booleanValue().should.equal(false)
//      response["message"].textValue().should.equal("The search failed with an exception: Found 2 instances of MaturityDate. Values are (TradeMaturityDate, 2026-12-01), (InstrumentMaturityDate, 2025-12-01)")
   }

   @Test
   fun submitQueryForNoResultsReturnsEmptyStream() = runTest {
      val turbine =
         queryService.submitVyneQlQueryStreamingResponse("""find { Empty[] }""", ResultMode.TYPED, MediaType.APPLICATION_JSON_VALUE).testIn(this)

      val errorResponse = turbine.awaitItem()
      errorResponse.should.not.be.`null`
      (errorResponse as ValueWithTypeName).typeName.should.equal("com.orbitalhq.errors.ErrorMessage")
      (errorResponse as ValueWithTypeName).value.should.equal("No data sources were found that can return Empty[]")
      turbine.awaitComplete()
   }

   @Test
   fun `policy thrown errors propagated`() = runTest {
      stubService.addResponse(
         "getClients", vyne.parseJsonModel(
            "Client[]", """
            [{
               "clientId": 1,
               "clientName": "name"
            }]
         """.trimIndent()
         )
      )
      val response = queryService.submitVyneQlQuery(
         """find { Client[] }""".trimIndent(),
         ResultMode.TYPED,
         MediaType.APPLICATION_JSON_VALUE,
         authenticationWithRoles(listOf("QueryRunners"))
      ).block()

      StepVerifier.create(response!!.body)
         .expectSubscription()
         .expectErrorMatches {
            (it is OrbitalQueryException) && it.message == "Not Authorized"
         }
         .verify()
   }

   @Test
   fun `policy thrown errors propagated for queries with function expressions`() = runTest {
      stubService.addResponse(
         "getClients", vyne.parseJsonModel(
            "Client[]", """
            [{
               "clientId": 1,
               "clientName": "name"
            }]
         """.trimIndent()
         )
      )
      val response = queryService.submitVyneQlQuery(
         """find { first(Client[]) }""".trimIndent(),
         ResultMode.TYPED,
         MediaType.APPLICATION_JSON_VALUE,
         authenticationWithRoles(listOf("QueryRunners"))
      ).block()

      StepVerifier.create(response!!.body)
         .expectSubscription()
         .expectErrorMatches {
            (it is OrbitalQueryException) && it.message == "Not Authorized"
         }
         .verify()
   }


}

/**
 * Test helper to return the flow as a List of ValueWithTypeName instances
 */
suspend fun Flow<Any>.asSimpleQueryResultList(): List<ValueWithTypeName> {
   @Suppress("UNCHECKED_CAST")
   return this.toList() as List<ValueWithTypeName>
}


fun String.withoutWhitespace(): String {
   return this
      .lines()
      .map { it.trim().replace(" ", "") }
      .filter { it.isNotEmpty() }
      .joinToString("")
}
