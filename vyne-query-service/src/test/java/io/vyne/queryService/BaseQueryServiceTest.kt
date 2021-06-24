package io.vyne.queryService

//import io.vyne.testVyne
import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.mock
import io.vyne.StubService
import io.vyne.Vyne
import io.vyne.models.json.parseJsonModel
import io.vyne.query.Query
import io.vyne.query.QueryMode
import io.vyne.query.TypeNameListQueryExpression
import io.vyne.query.active.ActiveQueryMonitor
import io.vyne.queryService.history.QueryEventConsumer
import io.vyne.queryService.history.QueryEventObserver
import io.vyne.queryService.history.db.QueryHistoryDbWriter
import io.vyne.queryService.query.QueryService
import io.vyne.spring.SimpleVyneProvider
import io.vyne.testVyne
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
         type EmptyId inherits String

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

         model Empty {
            id: EmptyId
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
   lateinit var vyne: Vyne
   lateinit var queryEventObserver: QueryEventObserver

   protected fun mockHistoryWriter(): QueryHistoryDbWriter {
      val eventConsumer: QueryEventConsumer = mock {}
      val historyWriter: QueryHistoryDbWriter = mock {
         on { createEventConsumer() } doReturn eventConsumer
      }
      return historyWriter
   }

   protected fun setupTestService(
      historyDbWriter: QueryHistoryDbWriter = mockHistoryWriter()
   ) {
      val (vyne, stubService) = testVyne(testSchema)
      setupTestService(vyne, stubService, historyDbWriter)
      prepareStubService(stubService, vyne)
   }

   protected fun setupTestService(
      vyne: Vyne,
      stubService: StubService?,
      historyDbWriter: QueryHistoryDbWriter = mockHistoryWriter()
   ): QueryService {
      if (stubService != null) {
         this.stubService = stubService
      }
      this.vyne = vyne
      queryService = QueryService(
         SimpleVyneProvider(vyne),
         historyDbWriter,
         Jackson2ObjectMapperBuilder().build(),
         ActiveQueryMonitor()
      )
      return queryService
   }

   protected fun prepareStubService(stubService: StubService, vyne: Vyne) {
      stubService.addResponse(
         "getOrders", vyne.parseJsonModel(
            "Order[]", """
            [
               {
                  "orderId": "orderId_0",
                  "traderName": "john",
                  "instrumentId": "Instrument_0"
               }
            ]
            """.trimIndent()
         ), modifyDataSource = true
      )

      val maturityDateTrade = "2026-12-01"
      stubService.addResponse(
         "getTrades", vyne.parseJsonModel(
            "Trade[]", """
               [{
                  "maturityDate": "$maturityDateTrade",
                  "orderId": "orderId_0",
                  "tradeId": "Trade_0"
               }]
            """.trimIndent()
         ), modifyDataSource = true
      )

      stubService.addResponse(
         "getInstrument", vyne.parseJsonModel(
            "Instrument", """
               {
                  "instrumentId": "Instrument_0",
                  "name": "2040-11-20 0.1 Bond"
               }
            """.trimIndent()
         ), modifyDataSource = true
      )
   }


   protected fun buildQuery(type: String) = Query(
      TypeNameListQueryExpression(listOf(type)),
      emptyMap(),
      queryMode = QueryMode.GATHER
   )
}

fun ResponseEntity<StreamingResponseBody>.contentString(): String {
   val stream = ByteArrayOutputStream()
   this.body!!.writeTo(stream)
   return String(stream.toByteArray())
}
