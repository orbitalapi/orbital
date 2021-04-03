package io.vyne.queryService

import app.cash.turbine.test
import com.winterbe.expekt.expect
import com.winterbe.expekt.should
import io.vyne.models.json.parseJsonModel
import io.vyne.query.ResultMode
import io.vyne.schemas.fqn
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runBlockingTest
import me.eugeniomarletti.kotlin.metadata.shadow.utils.addToStdlib.safeAs
import org.junit.Ignore
import org.junit.Test
import org.springframework.http.MediaType
import kotlin.test.assertEquals
import kotlin.time.ExperimentalTime

@ExperimentalCoroutinesApi
class QueryServiceTest : BaseQueryServiceTest() {


   @Test
   fun submitQueryJsonSimple() = runBlockingTest {

      val query = buildQuery("Order[]")
      val response = queryService.submitQuery(query, ResultMode.SIMPLE, MediaType.APPLICATION_JSON_VALUE)
         .body!!
         .asSimpleQueryResultList()
      response.should.not.be.empty
      response[0].typeName.should.equal("Order[]".fqn())
      response[0].value.safeAs<List<Any>>().should.have.size(1)
   }

   @ExperimentalTime
   @Test
   fun `when request type is csv then result mode is ignored`() = runBlockingTest {

      val query = buildQuery("Order[]")
      queryService.submitQuery(query, ResultMode.SIMPLE, TEXT_CSV)
         .body.test {
            assertEquals("orderId,traderName,instrumentId", (expectItem() as String).trim())
            assertEquals("orderId_0,john,Instrument_0",  (expectItem() as String).trim())
            expectComplete()
         }
   }


   @Test
   fun submitQueryJsonRaw() = runBlockingTest {

      val query = buildQuery("Order[]")
      val response = queryService.submitQuery(query, ResultMode.RAW, MediaType.APPLICATION_JSON_VALUE)
         .body
         .toList()

      TODO("Assert the contents of the list")
//      response[0]["orderId"].textValue().should.equal("orderId_0")
//      response[0]["traderName"].textValue().should.equal("john")
//      response[0]["instrumentId"].textValue().should.equal("Instrument_0")

   }

   @Test
   fun submitQueryCsvRaw() = runBlockingTest {

      val query = buildQuery("Order[]")
      val responseStr = queryService.submitQuery(query, ResultMode.RAW, TEXT_CSV)
         .body
         .toList()
      val csv = """
orderId,traderName,instrumentId
orderId_0,john,Instrument_0
""".trimIndent()
      TODO("Assert the contents of the list")
//      assertEquals(csv, responseStr.trimIndent())
   }


   @Test
   fun submitVyneQLQueryJsonSimple() = runBlockingTest {

      val response = queryService.submitVyneQlQuery(
         """findAll { Order[] } as Report[]""".trimIndent(),
         ResultMode.SIMPLE,
         MediaType.APPLICATION_JSON_VALUE
      )
         .body
         .toList()
      TODO("Assert the contents of the list")
//      val response = jacksonObjectMapper().readTree(responseStr)
//      response["fullyResolved"].booleanValue().should.equal(true)
//      response["results"].should.not.be.`null`
//      val resultList = response["results"]["lang.taxi.Array<Report>"] as ArrayNode
//      resultList.first()["maturityDate"].textValue().should.be.equal("2026-12-01")
   }

   @Test
   fun submitVyneQLQueryCsvSimple() = runBlockingTest {

      val responseStr =
         queryService.submitVyneQlQuery("""findAll { Order[] } as Report[]""".trimIndent(), ResultMode.SIMPLE, TEXT_CSV)
            .body.toList()

      TODO("Assert the contents of the list")
   }

   @Test
   fun submitVyneQLQueryJsonRaw() = runBlockingTest {

      stubService.addResponse(
         "getOrders", vyne.parseJsonModel(
            "Order[]", """
         [
            {
               "orderId": "orderId_0",
               "traderName": "john",
               "instrumentId": "Instrument_0"
            },
            {
               "orderId": "orderId_1",
               "instrumentId": "Instrument_1"
            }
         ]
         """.trimIndent()
         )
      )

      val response = queryService.submitVyneQlQuery(
         """findAll { Order[] } """.trimIndent(),
         ResultMode.RAW,
         MediaType.APPLICATION_JSON_VALUE
      )
         .body.toList()
      TODO("Assert the contents of the list")
//      val response = jacksonObjectMapper().readTree(responseStr)
//
//      response.isArray.should.be.`true`
//      (response as ArrayNode).size().should.equal(2)
//
//      response[0]["orderId"].textValue().should.equal("orderId_0")
//      response[0]["traderName"].textValue().should.equal("john")
//      response[0]["instrumentId"].textValue().should.equal("Instrument_0")
//
//      response[1]["orderId"].textValue().should.equal("orderId_1")
//      response[1]["instrumentId"].textValue().should.equal("Instrument_1")
//      response[1]["traderName"].should.be.`null`
   }

   @Test
   fun submitVyneQLQueryCsvRaw() = runBlockingTest {

      stubService.addResponse(
         "getOrders", vyne.parseJsonModel(
            "Order[]", """
         [
            {
               "orderId": "orderId_0",
               "traderName": "john",
               "instrumentId": "Instrument_0"
            },
            {
               "orderId": "orderId_1",
               "traderName": "pierre"
            }
         ]
         """.trimIndent()
         )
      )

      val responseStr =
         queryService.submitVyneQlQuery("""findAll { Order[] } """.trimIndent(), ResultMode.RAW, TEXT_CSV)
            .body.toList()
      TODO("Assert the contents of the list")
//      val csv = """
//orderId,traderName,instrumentId
//orderId_0,john,Instrument_0
//orderId_1,pierre,
//""".trimIndent()
//
//      assertEquals(csv, responseStr.trimIndent())
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

private suspend fun Flow<Any>.asSimpleQueryResultList():List<FirstEntryMetadataResultSerializer.ValueWithTypeName> {
   @Suppress("UNCHECKED_CAST")
   return this.toList() as List<FirstEntryMetadataResultSerializer.ValueWithTypeName>
}
