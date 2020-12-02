package io.vyne.queryService

import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.whenever
import io.vyne.StubService
import io.vyne.Vyne
import io.vyne.models.json.parseJsonModel
import io.vyne.query.Query
import io.vyne.query.QueryMode
import io.vyne.query.TypeNameListQueryExpression
import io.vyne.spring.VyneFactory
import io.vyne.testVyne
import org.junit.Before
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody
import java.io.ByteArrayOutputStream

abstract class BaseQueryServiceTest {
   companion object {
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
   }

   lateinit var queryService: QueryService
   lateinit var stubService: StubService
   lateinit var queryHistory: QueryHistory
   lateinit var vyne: Vyne

   @Before
   open fun setup() {
      val (vyne, stubService) = testVyne(testSchema)
      this.stubService = stubService
      this.vyne = vyne
      queryHistory = InMemoryQueryHistory()
      val mockVyneFactory = mock<VyneFactory>()
      whenever(mockVyneFactory.createVyne()).thenReturn(vyne)
      queryService = QueryService(mockVyneFactory, queryHistory, Jackson2ObjectMapperBuilder().build())

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


   protected fun buildQuery(type: String) = Query(
      TypeNameListQueryExpression(listOf(type)),
      emptyMap(),
      queryMode = QueryMode.GATHER)
}

fun ResponseEntity<StreamingResponseBody>.contentString():String {
   val stream = ByteArrayOutputStream()
   this.body!!.writeTo(stream)
   return String(stream.toByteArray())
}