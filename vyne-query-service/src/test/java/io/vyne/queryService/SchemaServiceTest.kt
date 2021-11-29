package io.vyne.queryService

import com.nhaarman.mockito_kotlin.mock
import com.winterbe.expekt.should
import io.vyne.models.csv.CsvFormatSpec
import io.vyne.schemaStore.SchemaSet
import io.vyne.schemaStore.SchemaSourceProvider
import io.vyne.schemaStore.SimpleSchemaStore
import io.vyne.schemas.ConsumedOperation
import io.vyne.schemas.QualifiedName
import io.vyne.schemas.taxi.TaxiSchema
import io.vyne.spring.SimpleTaxiSchemaProvider
import org.junit.Before
import org.junit.Test

class SchemaServiceTest {
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

         service ReferenceService {
            operation setUp()
         }

         service MultipleInvocationService {
            lineage {
               consumes operation ReferenceService.setUp
               stores Order
               stores Trade
            }
            operation getOrders(): Order[]
            operation getTrades(orderIds: OrderId): Trade
            operation getTrades(orderIds: OrderId[]): Trade[]
            operation getInstrument(instrumentId: InstrumentId): Instrument
         }
      """.trimIndent()
   val taxiSchema = TaxiSchema.compileOrFail(testSchema)
   val store = SimpleSchemaStore()

   val schemaService: SchemaService = SchemaService(
      SimpleTaxiSchemaProvider(testSchema),
      mock {},
      store,
      QueryServerConfig(),
   listOf(CsvFormatSpec))

   @Before
   fun setUp() {
      store.setSchemaSet(SchemaSet.from(taxiSchema.sources, 1))
   }

   @Test
   fun `can fetch service`() {
      val service = schemaService.getService("MultipleInvocationService")
      service.lineage.should.not.be.`null`
      service.lineage!!.consumes.should.equal(listOf(
         ConsumedOperation(serviceName = "ReferenceService", operationName = "setUp")
      ))
      val stores = service.lineage!!.stores.toSet()
      stores.should.equal(setOf(QualifiedName("Order"), QualifiedName("Trade")))
   }
}
