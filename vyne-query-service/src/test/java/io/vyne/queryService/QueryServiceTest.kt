package io.vyne.queryService

import app.cash.turbine.test
import app.cash.turbine.testIn
import com.winterbe.expekt.should
import io.vyne.models.json.parseJsonModel
import io.vyne.query.ResultMode
import io.vyne.query.ValueWithTypeName
import io.vyne.queryService.query.TEXT_CSV
import io.vyne.schemas.fqn
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runBlockingTest
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.springframework.http.MediaType
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
   fun submitQueryJsonSimple() = runBlocking {

      val query = buildQuery("Order[]")
      queryService.submitQuery(query, ResultMode.TYPED, MediaType.APPLICATION_JSON_VALUE)
         .body
         .test {
            val next = awaitItem() as ValueWithTypeName
            next.typeName.should.equal("Order".fqn().parameterizedName)
            (next.value as Map<String, Any>).should.equal(
               mapOf(
                  "orderId" to "orderId_0",
                  "traderName" to "john",
                  "instrumentId" to "Instrument_0"
               )
            )
            awaitComplete()
         }

   }


   @Test
   fun `csv request produces expected results regardless of resultmode`() = runTest {
      val query = buildQuery("Order[]")
      ResultMode.values().forEach { resultMode ->
         val turbine = queryService.submitQuery(query, resultMode, TEXT_CSV).body.testIn(this)
         val expected = """orderId,traderName,instrumentId
orderId_0,john,Instrument_0""".trimMargin().withoutWhitespace()
         val next = turbine.awaitItem()
         assertEquals(expected, (next as String).withoutWhitespace())
         turbine.awaitComplete()
      }


   }

   @Test
   fun `csv with projection returns expected results`() = runTest {
      ResultMode.values().forEach { resultMode ->

         val turbine = queryService
            .submitVyneQlQuery(
               """find { Order[] } as Report[]""".trimIndent(),
               resultMode,
               TEXT_CSV
            ).body.testIn(this)
         val expected = """orderId,tradeId,instrumentName,maturityDate,traderName
orderId_0,Trade_0,2040-11-20 0.1 Bond,2026-12-01,john
               """.withoutWhitespace()
         val item = (turbine.awaitItem() as String).withoutWhitespace()
         item.should.equal(expected.withoutWhitespace())
         turbine.awaitComplete()
      }

   }


   @Test
   fun `taxiQl as simple json returns expected result`() = runTest {

      val turbine = queryService.submitVyneQlQuery(
         """find { Order[] }""".trimIndent(),
         ResultMode.TYPED,
         MediaType.APPLICATION_JSON_VALUE
      ).body.testIn(this)

      val next = turbine.awaitItem() as ValueWithTypeName
      next.value.should.equal(
         mapOf(
            "orderId" to "orderId_0",
            "traderName" to "john",
            "instrumentId" to "Instrument_0"
         )
      )
      turbine.awaitComplete()
   }

   @Test
   fun `taxiQl as raw json returns raw map`() = runTest {

      val turbine = queryService.submitVyneQlQuery(
         """find { Order[] }""".trimIndent(),
         ResultMode.RAW,
         MediaType.APPLICATION_JSON_VALUE
      ).body.testIn(this)
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

      val turbine = queryService.submitVyneQlQuery(
         """find { Order[] } as Report[]""".trimIndent(),
         ResultMode.TYPED,
         MediaType.APPLICATION_JSON_VALUE
      )
         .body.testIn(this)
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

      val turbine = queryService.submitVyneQlQuery(
         """find { Order[] } as Report[]""".trimIndent(),
         ResultMode.RAW,
         MediaType.APPLICATION_JSON_VALUE
      )
         .body.testIn(this, timeout = Duration.INFINITE)
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
         .body.toList()
      TODO("Assert the contents of the list")
//      val response = jacksonObjectMapper().readTree(responseStr)
//      response["fullyResolved"].booleanValue().should.equal(false)
//      response["message"].textValue().should.equal("The search failed with an exception: Found 2 instances of MaturityDate. Values are (TradeMaturityDate, 2026-12-01), (InstrumentMaturityDate, 2025-12-01)")
   }

   @Test
   fun submitQueryForNoResultsReturnsEmptyStream() = runTest {
      val query = buildQuery("Empty[]")
      val turbine =
         queryService.submitQuery(query, ResultMode.TYPED, MediaType.APPLICATION_JSON_VALUE).body.testIn(this)

      turbine.awaitError()
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
