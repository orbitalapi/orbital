package io.vyne.queryService

import app.cash.turbine.test
import com.winterbe.expekt.should
import io.vyne.models.json.parseJsonModel
import io.vyne.query.ResultMode
import io.vyne.schemas.fqn
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runBlockingTest
import me.eugeniomarletti.kotlin.metadata.shadow.utils.addToStdlib.safeAs
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.springframework.http.MediaType
import kotlin.test.assertEquals
import kotlin.time.ExperimentalTime

@ExperimentalCoroutinesApi
@ExperimentalTime
class QueryServiceTest : BaseQueryServiceTest() {

   @Before
   fun setup() {
      setupTestService()
   }

   @Test
   fun submitQueryJsonSimple() = runBlockingTest {

      val query = buildQuery("Order[]")
      val response = queryService.submitQuery(query, ResultMode.SIMPLE, MediaType.APPLICATION_JSON_VALUE)
         .body!!
         .asSimpleQueryResultList()
      response.should.not.be.empty
      response[0].typeName.should.equal("Order[]".fqn().parameterizedName)
      response[0].value.safeAs<List<Any>>().should.have.size(1)
   }


   @Test
   fun `csv request produces expected results regardless of resultmode`() = runBlockingTest {

      val query = buildQuery("Order[]")
      ResultMode.values().forEach { resultMode ->
         queryService.submitQuery(query, resultMode, TEXT_CSV)
            .body.test {
               assertEquals("orderId,traderName,instrumentId", (expectItem() as String).trim())
               assertEquals("orderId_0,john,Instrument_0", (expectItem() as String).trim())
               expectComplete()
            }
      }

   }

   @Test
   fun `csv with projection returns expected results`() = runBlockingTest {

      ResultMode.values().forEach { resultMode ->
         val result = queryService.submitVyneQlQuery(
            """findAll { Order[] } as Report[]""".trimIndent(),
            resultMode,
            TEXT_CSV
         )
            .body.toList()
         result.should.have.elements(
            "orderId,tradeId,instrumentName,maturityDate,traderName\r\n",
            "orderId_0,,john\r\n"
         )
      }

   }


   @Test
   fun `taxiQl as simple json returns expected result`() = runBlockingTest {

      val response = queryService.submitVyneQlQuery(
         """findAll { Order[] }""".trimIndent(),
         ResultMode.SIMPLE,
         MediaType.APPLICATION_JSON_VALUE
      )
         .body
         .toList()
      response.should.have.size(1)
      val resultRow = response[0] as FirstEntryMetadataResultSerializer.ValueWithTypeName
      resultRow.value.should.equal(
         mapOf(
            "orderId" to "orderId_0",
            "traderName" to "john",
            "instrumentId" to "Instrument_0"
         )
      )
   }

   @Test
   fun `taxiQl as raw json returns raw map`() = runBlockingTest {

      val response = queryService.submitVyneQlQuery(
         """findAll { Order[] }""".trimIndent(),
         ResultMode.RAW,
         MediaType.APPLICATION_JSON_VALUE
      )
         .body
         .first()
      response.should.equal(
         mapOf(
            "orderId" to "orderId_0",
            "traderName" to "john",
            "instrumentId" to "Instrument_0"
         )
      )
   }

   @Test
   fun `taxiQl as simple json with projection returns expected result`() = runBlockingTest {

      val response = queryService.submitVyneQlQuery(
         """findAll { Order[] } as Report[]""".trimIndent(),
         ResultMode.SIMPLE,
         MediaType.APPLICATION_JSON_VALUE
      )
         .body
         .toList()
      response.should.have.size(1)
      val resultRow = response[0] as FirstEntryMetadataResultSerializer.ValueWithTypeName
      resultRow.value.should.equal(
         mapOf(
            "orderId" to "orderId_0",
            "tradeId" to null,
            "instrumentName" to null,
            "maturityDate" to null,
            "traderName" to "john"
         )
      )
   }

   @Test
   fun `taxiQl as raw json with projection returns expected result`() = runBlockingTest {

      val response = queryService.submitVyneQlQuery(
         """findAll { Order[] } as Report[]""".trimIndent(),
         ResultMode.RAW,
         MediaType.APPLICATION_JSON_VALUE
      )
         .body
         .toList()
      response.should.have.size(1)
      response[0].should.equal(
         mapOf(
            "orderId" to "orderId_0",
            "tradeId" to null,
            "instrumentName" to null,
            "maturityDate" to null,
            "traderName" to "john"
         )
      )
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
}

/**
 * Test helper to return the flow as a List of ValueWithTypeName instances
 */
suspend fun Flow<Any>.asSimpleQueryResultList(): List<FirstEntryMetadataResultSerializer.ValueWithTypeName> {
   @Suppress("UNCHECKED_CAST")
   return this.toList() as List<FirstEntryMetadataResultSerializer.ValueWithTypeName>
}

