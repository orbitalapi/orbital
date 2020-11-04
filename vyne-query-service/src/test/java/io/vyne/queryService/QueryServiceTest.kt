package io.vyne.queryService

import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.winterbe.expekt.should
import io.vyne.models.json.parseJsonModel
import io.vyne.query.ResultMode
import org.junit.Ignore
import org.junit.Test
import org.springframework.http.MediaType
import kotlin.test.assertEquals

class QueryServiceTest : BaseQueryServiceTest() {



   @Test
   fun `submitQueryJsonSimple`() {

      val query = buildQuery("Order[]")
      val responseStr = queryService.submitQuery(query, ResultMode.SIMPLE, MediaType.APPLICATION_JSON_VALUE)
         .contentString()
      val response = jacksonObjectMapper().readTree(responseStr)
      response["fullyResolved"].booleanValue().should.equal(true)
      response["results"].should.not.be.`null`

   }

   @Test
   fun `submitQueryCsvSimple`() {

      val query = buildQuery("Order[]")
      val responseStr = queryService.submitQuery(query, ResultMode.SIMPLE, TEXT_CSV)
         .contentString()
      // Simple CSV should still be json
      val response = jacksonObjectMapper().readTree(responseStr)
      response["fullyResolved"].booleanValue().should.equal(true)
      response["results"].should.not.be.`null`
   }

   @Test
   fun `submitQueryJsonRaw`() {

      val query = buildQuery("Order[]")
      val responseStr = queryService.submitQuery(query, ResultMode.RAW, MediaType.APPLICATION_JSON_VALUE)
         .contentString()
      val response = jacksonObjectMapper().readTree(responseStr)

      response[0]["orderId"].textValue().should.equal("orderId_0")
      response[0]["traderName"].textValue().should.equal("john")
      response[0]["instrumentId"].textValue().should.equal("Instrument_0")

   }

   @Test
   fun `submitQueryCsvRaw`() {

      val query = buildQuery("Order[]")
      val responseStr = queryService.submitQuery(query, ResultMode.RAW, TEXT_CSV)
         .contentString()
      val csv = """
orderId,traderName,instrumentId
orderId_0,john,Instrument_0
""".trimIndent()

      assertEquals(csv, responseStr.trimIndent())
   }


   @Test
   fun `submitVyneQLQueryJsonSimple`() {

      val responseStr = queryService.submitVyneQlQuery("""findAll { Order[] } as Report[]""".trimIndent(), ResultMode.SIMPLE, MediaType.APPLICATION_JSON_VALUE)
         .contentString()
      val response = jacksonObjectMapper().readTree(responseStr)
      response["fullyResolved"].booleanValue().should.equal(true)
      response["results"].should.not.be.`null`
      val resultList = response["results"]["lang.taxi.Array<Report>"] as ArrayNode
      resultList.first()["maturityDate"].textValue().should.be.equal("2026-12-01")
   }

   @Test
   fun `submitVyneQLQueryCsvSimple`() {

      val responseStr = queryService.submitVyneQlQuery("""findAll { Order[] } as Report[]""".trimIndent(), ResultMode.SIMPLE, TEXT_CSV)
         .contentString()
      // Simple CSV should still be json
      val response = jacksonObjectMapper().readTree(responseStr)
      response["fullyResolved"].booleanValue().should.equal(true)
      response["results"].should.not.be.`null`
   }

   @Test
   fun `submitVyneQLQueryJsonRaw`() {

      stubService.addResponse("getOrders", vyne.parseJsonModel("Order[]", """
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
         """.trimIndent()))

      val responseStr = queryService.submitVyneQlQuery("""findAll { Order[] } """.trimIndent(), ResultMode.RAW, MediaType.APPLICATION_JSON_VALUE)
         .contentString()
      val response = jacksonObjectMapper().readTree(responseStr)

      response.isArray.should.be.`true`
      (response as ArrayNode).size().should.equal(2)

      response[0]["orderId"].textValue().should.equal("orderId_0")
      response[0]["traderName"].textValue().should.equal("john")
      response[0]["instrumentId"].textValue().should.equal("Instrument_0")

      response[1]["orderId"].textValue().should.equal("orderId_1")
      response[1]["instrumentId"].textValue().should.equal("Instrument_1")
      response[1]["traderName"].should.be.`null`
   }

   @Test
   fun `submitVyneQLQueryCsvRaw`() {

      stubService.addResponse("getOrders", vyne.parseJsonModel("Order[]", """
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
         """.trimIndent()))

      val responseStr = queryService.submitVyneQlQuery("""findAll { Order[] } """.trimIndent(), ResultMode.RAW, TEXT_CSV)
         .contentString()

      val csv = """
orderId,traderName,instrumentId
orderId_0,john,Instrument_0
orderId_1,pierre,
""".trimIndent()

      assertEquals(csv, responseStr.trimIndent())
   }

   @Ignore("LENS-345 has been opened to re-enable this functionality")
   @Test
   fun `duplicate match error in projection returned as part of failed response`() {

      val maturityDateInstrument = "2025-12-01"
      stubService.addResponse("getInstrument", vyne.parseJsonModel("Instrument", """
            {
               "maturityDate": "$maturityDateInstrument",
               "instrumentId": "Instrument_0",
               "name": "2040-11-20 0.1 Bond"
            }
         """.trimIndent()))

      val responseStr = queryService.submitVyneQlQuery("""findAll { Order[] } as Report[]""".trimIndent(), ResultMode.SIMPLE, MediaType.APPLICATION_JSON_VALUE)
         .contentString()

      val response = jacksonObjectMapper().readTree(responseStr)
      response["fullyResolved"].booleanValue().should.equal(false)
      response["message"].textValue().should.equal("The search failed with an exception: Found 2 instances of MaturityDate. Values are (TradeMaturityDate, 2026-12-01), (InstrumentMaturityDate, 2025-12-01)")
   }
}

