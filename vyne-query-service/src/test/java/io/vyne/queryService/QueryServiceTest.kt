package io.vyne.queryService

import app.cash.turbine.test
import com.nhaarman.mockito_kotlin.doThrow
import com.winterbe.expekt.expect
import com.winterbe.expekt.should
import io.vyne.models.json.parseJsonModel
import io.vyne.query.ResultMode
import io.vyne.query.ValueWithTypeName
import io.vyne.queryService.query.FirstEntryMetadataResultSerializer
import io.vyne.queryService.query.TEXT_CSV
import io.vyne.schemas.fqn
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.flow.zip
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.springframework.http.MediaType
import kotlin.test.assertEquals
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.seconds

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
      queryService.submitQuery(query, ResultMode.SIMPLE, MediaType.APPLICATION_JSON_VALUE)
         .body
         .test {
            val next = expectItem() as ValueWithTypeName
            next.typeName.should.equal("Order".fqn().parameterizedName)
            (next.value as Map<String, Any>).should.equal(
               mapOf(
                  "orderId" to "orderId_0",
                  "traderName" to "john",
                  "instrumentId" to "Instrument_0"
               )
            )
            expectComplete()
         }

   }


   @Test
   fun `csv request produces expected results regardless of resultmode`() = runBlocking {

      val query = buildQuery("Order[]")

      ResultMode.values().forEach { resultMode ->
         queryService.submitQuery(query, resultMode, TEXT_CSV)
            .body.test(timeout = Duration.ZERO) {
               val expected = """orderId,traderName,instrumentId
orderId_0,john,Instrument_0""".trimMargin().withoutWhitespace()
               val next = expectItem()
               assertEquals(expected, (next as String).withoutWhitespace())
               expectComplete()
            }
      }

   }

   @Test
      fun `csv with projection returns expected results`() = runBlocking {

      ResultMode.values().forEach { resultMode ->
         val result = queryService.submitVyneQlQuery(
            """findAll { Order[] } as Report[]""".trimIndent(),
            resultMode,
            TEXT_CSV
         )
            .body.test(timeout = Duration.ZERO) {
               val expected = """orderId,tradeId,instrumentName,maturityDate,traderName
orderId_0,Trade_0,2040-11-20 0.1 Bond,2026-12-01,john
               """.withoutWhitespace()
               val item = (expectItem() as String).withoutWhitespace()
               item.should.equal(expected.withoutWhitespace())
               expectComplete()
            }
      }

   }


   @Test
   fun `taxiQl as simple json returns expected result`() = runBlocking {

      val response = queryService.submitVyneQlQuery(
         """findAll { Order[] }""".trimIndent(),
         ResultMode.SIMPLE,
         MediaType.APPLICATION_JSON_VALUE
      )
         .body
         .test {
            val next = expectItem() as ValueWithTypeName
            next.value.should.equal(
               mapOf(
                  "orderId" to "orderId_0",
                  "traderName" to "john",
                  "instrumentId" to "Instrument_0"
               )
            )
            expectComplete()
         }
   }

   @Test
   fun `taxiQl as raw json returns raw map`() = runBlocking {

      val response = queryService.submitVyneQlQuery(
         """findAll { Order[] }""".trimIndent(),
         ResultMode.RAW,
         MediaType.APPLICATION_JSON_VALUE
      )
         .body
         .test {
            val next = expectItem() as Map<String,Any?>
            next.should.equal(
               mapOf(
                  "orderId" to "orderId_0",
                  "traderName" to "john",
                  "instrumentId" to "Instrument_0"
               )
            )
            expectComplete()
         }
   }

   @Test
   fun `taxiQl as simple json with projection returns expected result`() = runBlocking {

      val response = queryService.submitVyneQlQuery(
         """findAll { Order[] } as Report[]""".trimIndent(),
         ResultMode.SIMPLE,
         MediaType.APPLICATION_JSON_VALUE
      )
         .body
         .test {
            val next = expectItem() as ValueWithTypeName
            next.value.should.equal(
               mapOf(
                  "orderId" to "orderId_0",
                  "tradeId" to "Trade_0",
                  "instrumentName" to "2040-11-20 0.1 Bond",
                  "maturityDate" to "2026-12-01",
                  "traderName" to "john"
               )
            )
            expectComplete()
         }
   }

   @Test
   fun `taxiQl as raw json with projection returns expected result`() = runBlocking {

      val response = queryService.submitVyneQlQuery(
         """findAll { Order[] } as Report[]""".trimIndent(),
         ResultMode.RAW,
         MediaType.APPLICATION_JSON_VALUE
      )
         .body
         .test(5.seconds) {
            val next = expectItem() as Map<String, Any?>
            next.should.equal(
               mapOf(
                  "orderId" to "orderId_0",
                  "tradeId" to "Trade_0",
                  "instrumentName" to "2040-11-20 0.1 Bond",
                  "maturityDate" to "2026-12-01",
                  "traderName" to "john"
               )
            )
            expectComplete()
         }
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
         """findAll { Order[] } as Report[]""".trimIndent(),
         ResultMode.SIMPLE,
         MediaType.APPLICATION_JSON_VALUE
      )
         .body.toList()
      TODO("Assert the contents of the list")
//      val response = jacksonObjectMapper().readTree(responseStr)
//      response["fullyResolved"].booleanValue().should.equal(false)
//      response["message"].textValue().should.equal("The search failed with an exception: Found 2 instances of MaturityDate. Values are (TradeMaturityDate, 2026-12-01), (InstrumentMaturityDate, 2025-12-01)")
   }

   @Test
   fun submitQueryForNoResultsReturnsEmptyStream() = runBlocking {

      val query = buildQuery("Empty[]")
      queryService.submitQuery(query, ResultMode.SIMPLE, MediaType.APPLICATION_JSON_VALUE)
         .body
         .test {
            expectError()
         }

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
      .map { it.trim().replace(" ","") }
      .filter { it.isNotEmpty() }
      .joinToString("")
}
