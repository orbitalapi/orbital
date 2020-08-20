package io.vyne.queryService

import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.whenever
import com.winterbe.expekt.should
import io.vyne.StubService
import io.vyne.Vyne
import io.vyne.models.json.parseJsonModel
import io.vyne.query.*
import io.vyne.schemas.taxi.TaxiSchema
import io.vyne.spring.VyneFactory
import org.junit.Before
import org.junit.Test
import org.springframework.http.MediaType
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder
import kotlin.test.assertEquals

class QueryServiceTest {
   fun testVyne(schema: TaxiSchema): Pair<Vyne, StubService> {
      val stubService = StubService()
      val queryEngineFactory = QueryEngineFactory.withOperationInvokers(stubService)
      val vyne = Vyne(queryEngineFactory).addSchema(schema)
      return vyne to stubService
   }

   fun testVyne(schema: String) = testVyne(TaxiSchema.from(schema))

   val testSchema = """
         type OrderId inherits String
         type TraderName inherits String
         type InstrumentId inherits String
         type MaturityDate inherits Date
         type InstrumentMaturityDate inherits MaturityDate
         type TradeMaturityDate inherits MaturityDate
         type TradeId inherits String
         type InstrumentName inherits String
         model Order {
            orderId: OrderId
            traderName : TraderName
            instrumentId: InstrumentId
         }
         model Instrument {
             instrumentId: InstrumentId
             maturityDate: InstrumentMaturityDate
             name: InstrumentName
         }
         model Trade {
            orderId: OrderId
            maturityDate: TradeMaturityDate
            tradeId: TradeId
         }

         model Report {
            orderId: OrderId
            tradeId: TradeId
            instrumentName: InstrumentName
            maturityDate: MaturityDate
            traderName : TraderName
         }

         service MultipleInvocationService {
            operation getOrders(): Order[]
            operation getTrades(orderIds: OrderId): Trade
            operation getTrades(orderIds: OrderId[]): Trade[]
            operation getInstrument(instrumentId: InstrumentId): Instrument
         }
      """.trimIndent()

   lateinit var queryService: QueryService
   lateinit var stubService: StubService
   lateinit var vyne: Vyne

   @Before
   fun setup() {
      stubService = StubService()
      val queryEngineFactory = QueryEngineFactory.withOperationInvokers(stubService)
      vyne = Vyne(queryEngineFactory).addSchema(TaxiSchema.from(testSchema))
      val mockVyneFactory = mock<VyneFactory>()
      whenever(mockVyneFactory.createVyne()).thenReturn(vyne)
      queryService = QueryService(mockVyneFactory, NoopQueryHistory(), Jackson2ObjectMapperBuilder().build())

      stubService.addResponse("getOrders", vyne.parseJsonModel("Order[]", """
         [
            {
               "orderId": "orderId_0",
               "traderName": "john",
               "instrumentId": "Instrument_0"
            }
         ]
         """.trimIndent()))

      val maturityDateTrade = "2026-12-01"
      stubService.addResponse("getTrades", vyne.parseJsonModel("Trade[]", """
            [{
               "maturityDate": "$maturityDateTrade",
               "orderId": "orderId_0",
               "tradeId": "Trade_0"
            }]
         """.trimIndent()))

      stubService.addResponse("getInstrument", vyne.parseJsonModel("Instrument", """
            {
               "instrumentId": "Instrument_0",
               "name": "2040-11-20 0.1 Bond"
            }
         """.trimIndent()))
   }

   @Test
   fun `submitQueryJsonSimple`() {

      val query = buildQuery("Order[]", ResultMode.SIMPLE)
      val responseStr = queryService.submitQuery(query, MediaType.APPLICATION_JSON_VALUE)

      val response = jacksonObjectMapper().readTree(responseStr)
      response["fullyResolved"].booleanValue().should.equal(true)
      response["results"].should.not.be.`null`

   }

   @Test
   fun `submitQueryCsvSimple`() {

      val query = buildQuery("Order[]", ResultMode.SIMPLE)
      val responseStr = queryService.submitQuery(query, TEXT_CSV)

      // Simple CSV should still be json
      val response = jacksonObjectMapper().readTree(responseStr)
      response["fullyResolved"].booleanValue().should.equal(true)
      response["results"].should.not.be.`null`
   }

   @Test
   fun `submitQueryJsonRaw`() {

      val query = buildQuery("Order[]", ResultMode.RAW)
      val responseStr = queryService.submitQuery(query, MediaType.APPLICATION_JSON_VALUE)

      val response = jacksonObjectMapper().readTree(responseStr)

      response[0]["orderId"].textValue().should.equal("orderId_0")
      response[0]["traderName"].textValue().should.equal("john")
      response[0]["instrumentId"].textValue().should.equal("Instrument_0")

   }

   @Test
   fun `submitQueryCsvRaw`() {

      val query = buildQuery("Order[]", ResultMode.RAW)
      val responseStr = queryService.submitQuery(query, TEXT_CSV)

      val csv = """
orderId,traderName,instrumentId
orderId_0,john,Instrument_0
""".trimIndent()

      assertEquals(csv, responseStr.trimIndent())
   }

   private fun buildQuery(type: String, resultMode: ResultMode) = Query(
      TypeNameListQueryExpression(listOf(type)),
      emptyMap(),
      queryMode = QueryMode.GATHER,
      resultMode = resultMode)


   @Test
   fun `submitVyneQLQueryJsonSimple`() {

      val responseStr = queryService.submitVyneQlQuery("""findAll { Order[] } as Report[]""".trimIndent(), ResultMode.SIMPLE, MediaType.APPLICATION_JSON_VALUE)

      val response = jacksonObjectMapper().readTree(responseStr)
      response["fullyResolved"].booleanValue().should.equal(true)
      response["results"].should.not.be.`null`
      val resultList = response["results"]["lang.taxi.Array<Report>"] as ArrayNode
      resultList.first()["maturityDate"].textValue().should.be.equal("2026-12-01")
   }

   @Test
   fun `submitVyneQLQueryCsvSimple`() {

      val responseStr = queryService.submitVyneQlQuery("""findAll { Order[] } as Report[]""".trimIndent(), ResultMode.SIMPLE, TEXT_CSV)

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

      val csv = """
orderId,traderName,instrumentId
orderId_0,john,Instrument_0
orderId_1,pierre
""".trimIndent()

      assertEquals(csv, responseStr.trimIndent())
   }

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

      val response = jacksonObjectMapper().readTree(responseStr)
      response["fullyResolved"].booleanValue().should.equal(false)
      response["message"].textValue().should.equal("The search failed with an exception: Found 2 instances of MaturityDate. Values are (TradeMaturityDate, 2026-12-01), (InstrumentMaturityDate, 2025-12-01)")
   }
}
