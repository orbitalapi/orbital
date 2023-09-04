package com.orbitalhq.queryService

import com.winterbe.expekt.should
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import com.orbitalhq.PackageIdentifier
import com.orbitalhq.asPackage
import com.orbitalhq.cockpit.core.schemas.SchemaService
import com.orbitalhq.models.csv.CsvFormatSpec
import com.orbitalhq.schema.api.SchemaSet
import com.orbitalhq.schema.api.SimpleSchemaProvider
import com.orbitalhq.schema.consumer.SimpleSchemaStore
import com.orbitalhq.schema.spring.SimpleTaxiSchemaProvider
import com.orbitalhq.schemas.ConsumedOperation
import com.orbitalhq.schemas.QualifiedName
import com.orbitalhq.schemas.fqn
import com.orbitalhq.schemas.taxi.TaxiSchema
import com.orbitalhq.toParsedPackages
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
      store,
      listOf(CsvFormatSpec)
   )

   @Before
   fun setUp() {
      store.setSchemaSet(SchemaSet.fromParsed(taxiSchema.sources.asPackage().toParsedPackages(), 1))
   }

   @Test
   fun `can fetch service`() {
      val service = schemaService.getService("MultipleInvocationService")
      service.lineage.should.not.be.`null`
      service.lineage!!.consumes.should.equal(
         listOf(
            ConsumedOperation(serviceName = "ReferenceService", operationName = "setUp")
         )
      )
      val stores = service.lineage!!.stores.toSet()
      stores.should.equal(
         setOf(
            QualifiedName.from("Order"), QualifiedName.from(
               "Trade"
            )
         )
      )
   }

   @Test
   fun `returns saved queries`() {
      val querySrc = """query FindFilm(id:FilmId) {
            find { Film(FilmId == id) }
         }"""
      val src = """
         model Film {
            id : FilmId inherits String
         }
         $querySrc
      """.trimIndent()
      val schema = TaxiSchema.from(src)
      val service = createService(schema)
      val queries = service.getSavedQueries()
      queries.shouldHaveSize(1)
      val query = queries.single()
      query.name.shouldBe("FindFilm".fqn())
      query.sources.single().content.withoutWhitespace().shouldBe(querySrc.withoutWhitespace())
   }

   private fun createService(schema: TaxiSchema): SchemaService {
      return SchemaService(
         SimpleSchemaProvider(schema, PackageIdentifier.fromId("com/test/foo:1.0.0")),
         SimpleSchemaStore(SchemaSet.from(schema, 0)),
         emptyList()
      )
   }
}
