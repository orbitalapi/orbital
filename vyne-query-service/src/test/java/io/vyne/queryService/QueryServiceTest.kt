package io.vyne.queryService

import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.whenever
import com.winterbe.expekt.should
import io.vyne.StubService
import io.vyne.Vyne
import io.vyne.models.json.parseJsonModel
import io.vyne.query.QueryEngineFactory
import io.vyne.query.ResultMode
import io.vyne.schemas.taxi.TaxiSchema
import io.vyne.spring.VyneFactory
import org.junit.Test

class QueryServiceTest {
   fun testVyne(schema: TaxiSchema): Pair<Vyne, StubService> {
      val stubService = StubService()
      val queryEngineFactory = QueryEngineFactory.withOperationInvokers(stubService)
      val vyne = Vyne(queryEngineFactory).addSchema(schema)
      return vyne to stubService
   }

   fun testVyne(schema: String) = testVyne(TaxiSchema.from(schema))
   @Test
   fun `duplicate match error in projection returned as part of failed response`() {
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
      val stubService = StubService()
      val queryEngineFactory = QueryEngineFactory.withOperationInvokers(stubService)
      val vyne = Vyne(queryEngineFactory).addSchema(TaxiSchema.from(testSchema))
      val mockVyneFactory = mock<VyneFactory>()
      whenever(mockVyneFactory.createVyne()).thenReturn(vyne)
      val queryService = QueryService(mockVyneFactory, NoopQueryHistory())
      stubService.addResponse("getOrders", vyne.parseJsonModel("Order[]", """
         [
            {
               "orderId": "orderId_0",
               "traderName": "john",
               "instrumentId": "Instrument_0"
            }
         ]
         """.trimIndent()))

      val maturityDateInstrument = "2025-12-01"
      val maturityDateTrade = "2026-12-01"
      stubService.addResponse("getInstrument", vyne.parseJsonModel("Instrument", """
            {
               "maturityDate": "$maturityDateInstrument",
               "instrumentId": "Instrument_0",
               "name": "2040-11-20 0.1 Bond"
            }
         """.trimIndent()))

      stubService.addResponse("getTrades", vyne.parseJsonModel("Trade[]", """
            [{
               "maturityDate": "$maturityDateTrade",
               "orderId": "orderId_0",
               "tradeId": "Trade_0"
            }]
         """.trimIndent()))
      val response = queryService.doVyneQlQuery("""findAll { Order[] } as Report[]""".trimIndent(), ResultMode.SIMPLE)
      response.isFullyResolved.should.be.`false`
      (response as FailedSearchResponse).message.should.be.equal("The search failed with an exception: Found 2 instances of MaturityDate. Values are (TradeMaturityDate, 2026-12-01), (InstrumentMaturityDate, 2025-12-01)")
   }
}
