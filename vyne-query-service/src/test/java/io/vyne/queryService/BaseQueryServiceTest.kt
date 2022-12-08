package io.vyne.queryService

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.mock
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import io.vyne.StubService
import io.vyne.Vyne
import io.vyne.VyneProvider
import io.vyne.history.QueryEventObserver
import io.vyne.history.db.QueryHistoryDbWriter
import io.vyne.models.TypedInstance
import io.vyne.models.csv.CsvFormatSpec
import io.vyne.models.json.parseJson
import io.vyne.models.json.parseJsonModel
import io.vyne.models.json.parseKeyValuePair
import io.vyne.query.HistoryEventConsumerProvider
import io.vyne.query.Query
import io.vyne.query.QueryEventConsumer
import io.vyne.query.QueryMode
import io.vyne.query.TypeNameListQueryExpression
import io.vyne.query.active.ActiveQueryMonitor
import io.vyne.queryService.query.MetricsEventConsumer
import io.vyne.queryService.query.QueryResponseFormatter
import io.vyne.queryService.query.QueryService
import io.vyne.schema.api.SimpleSchemaProvider
import io.vyne.spring.SimpleVyneProvider
import io.vyne.spring.config.TestDiscoveryClientConfig
import io.vyne.testVyne
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
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
   lateinit var meterRegistry: SimpleMeterRegistry

   protected fun mockHistoryWriter(): QueryHistoryDbWriter {
      val eventConsumer: QueryEventConsumer = mock {}
      val historyWriter: QueryHistoryDbWriter = mock {
         on { createEventConsumer(any(), any()) } doReturn eventConsumer
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
      historyDbWriter: HistoryEventConsumerProvider = mockHistoryWriter()
   ): QueryService {
      if (stubService != null) {
         this.stubService = stubService
      }
      this.vyne = vyne
      this.meterRegistry = SimpleMeterRegistry()
      queryService = QueryService(
         SimpleVyneProvider(vyne),
         historyDbWriter,
         Jackson2ObjectMapperBuilder().build(),
         ActiveQueryMonitor(),
         MetricsEventConsumer(this.meterRegistry),
         QueryResponseFormatter(listOf(CsvFormatSpec), SimpleSchemaProvider(vyne.schema))
      )
      return queryService
   }

   private fun prepareStubService(stubService: StubService, vyne: Vyne) {
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

@TestConfiguration
@Import(TestDiscoveryClientConfig::class)
class TestSpringConfig {
   @Bean
   @Primary
   fun vyneProvider(): VyneProvider {
      val (vyne, stub) = testVyne(
         """
         type EmailAddress inherits String
         type PersonId inherits Int
         type LoyaltyCardNumber inherits String
         type BalanceInPoints inherits Decimal
         model AccountBalance {
            balance: BalanceInPoints
         }
         service Service {
            operation findPersonIdByEmail(EmailAddress):PersonId
            operation findMembership(PersonId):LoyaltyCardNumber
            operation findBalance(LoyaltyCardNumber):AccountBalance
         }
      """
      )
      // setup stubs
      stub.addResponse("findPersonIdByEmail", TypedInstance.from(vyne.type("PersonId"), 1, vyne.schema), modifyDataSource = true)
      stub.addResponse(
         "findMembership",
         vyne.parseKeyValuePair("LoyaltyCardNumber", "1234-5678"),
         modifyDataSource = true
      )
      stub.addResponse("findBalance", vyne.parseJson("AccountBalance", """{ "balance" : 100 }"""), modifyDataSource = true)
      return SimpleVyneProvider(vyne)
   }
}
