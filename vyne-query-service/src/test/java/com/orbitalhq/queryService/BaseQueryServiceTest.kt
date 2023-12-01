package com.orbitalhq.queryService

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.mock
import com.orbitalhq.StubService
import com.orbitalhq.Vyne
import com.orbitalhq.VyneProvider
import com.orbitalhq.formats.csv.CsvFormatSpec
import com.orbitalhq.history.db.QueryHistoryDbWriter
import com.orbitalhq.metrics.NoOpMetricsReporter
import com.orbitalhq.metrics.QueryMetricsReporter
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.models.json.parseJson
import com.orbitalhq.models.json.parseJsonModel
import com.orbitalhq.models.json.parseKeyValuePair
import com.orbitalhq.query.HistoryEventConsumerProvider
import com.orbitalhq.query.Query
import com.orbitalhq.query.QueryEventConsumer
import com.orbitalhq.query.QueryMode
import com.orbitalhq.query.TypeNameListQueryExpression
import com.orbitalhq.query.runtime.core.QueryLifecycleEventObserver
import com.orbitalhq.query.runtime.core.QueryResponseFormatter
import com.orbitalhq.query.runtime.core.QueryService
import com.orbitalhq.query.runtime.core.monitor.ActiveQueryMonitor
import com.orbitalhq.schema.api.SimpleSchemaProvider
import com.orbitalhq.schemas.taxi.TaxiSchema
import com.orbitalhq.spring.SimpleVyneProvider
import com.orbitalhq.spring.config.TestDiscoveryClientConfig
import com.orbitalhq.testVyne
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.junit.jupiter.api.BeforeAll
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.io.ByteArrayOutputStream

@Testcontainers
abstract class BaseQueryServiceTest {
   companion object {
      @Container
      @ServiceConnection
      val postgres = PostgreSQLContainer<Nothing>("postgres:11.1") as PostgreSQLContainer<*>

      @BeforeAll
      fun setup() {
         postgres.start()
         postgres.waitingFor(Wait.forListeningPort())
      }

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
            @FirstNotEmpty
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
   lateinit var queryEventObserver: QueryLifecycleEventObserver
   lateinit var meterRegistry: SimpleMeterRegistry


   protected fun mockHistoryWriter(): QueryHistoryDbWriter {
      val eventConsumer: QueryEventConsumer = mock {}
      val historyWriter: QueryHistoryDbWriter = mock {
         on { createEventConsumer(any(), any()) } doReturn eventConsumer
      }
      return historyWriter
   }

   protected fun setupTestService(
      historyDbWriter: QueryHistoryDbWriter = mockHistoryWriter(),
      schema: String = testSchema,
      prepareStubCallback: (StubService, Vyne) -> Unit = { stub, vyne -> this.prepareStubService(stub, vyne) }
   ) {
      val (vyne, stubService) = testVyne(schema)
      setupTestService(vyne, stubService, historyDbWriter)
      prepareStubCallback(stubService, vyne)
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
         SimpleSchemaProvider(vyne.schema),
         SimpleVyneProvider(vyne),
         historyDbWriter,
         Jackson2ObjectMapperBuilder().build(),
         ActiveQueryMonitor(),
         QueryResponseFormatter(listOf(CsvFormatSpec), SimpleSchemaProvider(vyne.schema))
      )
      return queryService
   }

   public fun prepareStubService(stubService: StubService, vyne: Vyne) {
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
      val response = vyne.parseJsonModel(
         "Trade[]", """
               [{
                  "maturityDate": "$maturityDateTrade",
                  "orderId": "orderId_0",
                  "tradeId": "Trade_0"
               }]
            """.trimIndent()
      )
      stubService.addResponse(
         "getTrades", response, modifyDataSource = true
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
   fun taxiSchema(): TaxiSchema = TaxiSchema.from(
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

   @Bean
   fun metricsReporter() = NoOpMetricsReporter

   @Bean
   @Primary
   fun vyneProvider(taxiSchema: TaxiSchema): VyneProvider {
      val (vyne, stub) = testVyne(taxiSchema)
      // setup stubs
      stub.addResponse(
         "findPersonIdByEmail",
         TypedInstance.from(vyne.type("PersonId"), 1, vyne.schema),
         modifyDataSource = true
      )
      stub.addResponse(
         "findMembership",
         vyne.parseKeyValuePair("LoyaltyCardNumber", "1234-5678"),
         modifyDataSource = true
      )
      stub.addResponse(
         "findBalance",
         vyne.parseJson("AccountBalance", """{ "balance" : 100 }"""),
         modifyDataSource = true
      )
      return SimpleVyneProvider(vyne)
   }
}
