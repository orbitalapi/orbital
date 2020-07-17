package io.vyne.cask.services

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.argumentCaptor
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.times
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import com.winterbe.expekt.should
import io.vyne.cask.ingest.IngestionInitialisedEvent
import io.vyne.cask.query.generators.AfterTemporalOperationGenerator
import io.vyne.cask.query.generators.BeforeTemporalOperationGenerator
import io.vyne.cask.query.generators.BetweenTemporalOperationGenerator
import io.vyne.cask.query.generators.FindByFieldIdOperationGenerator
import io.vyne.cask.query.generators.FindByIdGenerators
import io.vyne.cask.query.generators.FindByMultipleGenerator
import io.vyne.cask.query.generators.FindBySingleResultGenerator
import io.vyne.schemaStore.SchemaProvider
import io.vyne.schemaStore.SchemaStoreClient
import io.vyne.schemas.Schema
import io.vyne.schemas.fqn
import io.vyne.schemas.taxi.TaxiSchema
import lang.taxi.types.PrimitiveType
import org.junit.Test

class CaskServiceSchemaGeneratorTest {
   val schemaProvider = mock<SchemaProvider>()
   val schemaStoreClient = mock<SchemaStoreClient>()
   val caskServiceSchemaWriter = CaskServiceSchemaWriter(schemaStoreClient)
   private val schema = """
    type alias Price as Decimal
    type alias Symbol as String
    type alias MaturityDate as Date
    type TransactionEventDateTime inherits Instant
    type OrderWindowSummary {
    @Association
    @Id
    symbol : Symbol by xpath("/Symbol")
    open : Price by xpath("/Open")
    high : Price by xpath("/High")
    close : Price by xpath("/Close")
    @Between
    @After
    @Before
    maturityDate: MaturityDate
    @Between
    @After
    @Before
    orderDateTime : TransactionEventDateTime( @format = "yyyy-MM-dd HH:mm:ss.SSSSSSS")
}

   """.trimIndent()

   @Test
   fun `schemas with formatted date types generate valid schemas`() {
      val schema = """
         model Trade {
            @Before
            tradeDate : Instant( @format = 'yyyy-mm-ddThh:mm:ss' )
         }
      """.trimIndent()
      val (serviceSchemaGenerator,taxiSchema) = schemaGeneratorFor(schema)
      val serviceSchema = argumentCaptor<String>()
      // When
      val generated = serviceSchemaGenerator.generateSchema(taxiSchema.versionedType("Trade".fqn()))
      val operation = generated.services.first().operation("findByTradeDateBefore")
      operation.parameters.first().type.qualifiedName.should.equal(PrimitiveType.INSTANT.qualifiedName)
   }

   private fun schemaGeneratorFor(schema: String): Pair<CaskServiceSchemaGenerator,TaxiSchema> {
      val typeSchema = lang.taxi.Compiler(schema).compile()
      val taxiSchema = TaxiSchema(typeSchema, listOf())
      whenever(schemaProvider.schema()).thenReturn(taxiSchema)
      return CaskServiceSchemaGenerator(
         schemaProvider,
         caskServiceSchemaWriter,
         listOf(
            FindByFieldIdOperationGenerator(),
            AfterTemporalOperationGenerator(),
            BeforeTemporalOperationGenerator(),
            BetweenTemporalOperationGenerator(),
            FindBySingleResultGenerator(),
            FindByMultipleGenerator(),
            FindByIdGenerators())) to taxiSchema

   }

   @Test
   fun `Cask generate service schema with correct imports`() {
      val simpleSchema = """[[
A price that a symbol was traded at
]]
type Price inherits Decimal
[[
The opening price at the beginning of a period
]]
type OpenPrice inherits Price
[[
The closing price at the end of a trading period
]]
type ClosePrice inherits Price
[[
The ticker for a tradable instrument
]]
type Symbol inherits String
type OrderWindowSummaryCsv {
    orderDate : DateTime( @format = 'yyyy-MM-dd hh-a' ) by column(1)
    @Id
    symbol : Symbol by column(2)
    open : Price by column(3)
    close : Price by column(4)
}"""
      // given
      val typeSchema = lang.taxi.Compiler(simpleSchema).compile()
      val taxiSchema = TaxiSchema(typeSchema, listOf())
      whenever(schemaProvider.schema()).thenReturn(taxiSchema)
      val (serviceSchemaGenerator, _) = schemaGeneratorFor(simpleSchema)
      val schemaName = argumentCaptor<String>()
      val schemaVersion = argumentCaptor<String>()
      val serviceSchema = argumentCaptor<String>()
      // When
      serviceSchemaGenerator.generateAndPublishService(taxiSchema.versionedType("OrderWindowSummaryCsv".fqn()))
      // Then
      verify(schemaStoreClient, times(1)).submitSchema(schemaName.capture(), schemaVersion.capture(), serviceSchema.capture())
      schemaName.firstValue.should.startWith("vyne.casks.OrderWindowSummaryCsv@")
      "1.0.0".should.equal(schemaVersion.firstValue)
      """
import OrderWindowSummaryCsv
import Symbol

namespace vyne.casks {



   @ServiceDiscoveryClient(serviceName = "cask")
   service OrderWindowSummaryCsvCaskService {
      @HttpOperation(method = "GET" , url = "/api/cask/findSingleBy/OrderWindowSummaryCsv/symbol/{id}")
      operation findSingleBySymbol( @PathVariable(name = "id") id : Symbol ) : OrderWindowSummaryCsv( Symbol = id )
   }
}

""".replace("\\s".toRegex(), "").should.equal(serviceSchema.firstValue.replace("\\s".toRegex(), ""))
   }

   @Test
   fun `Cask generates service schema from valid type schema`() {
      // given
      val typeSchema = lang.taxi.Compiler(schema).compile()
      val taxiSchema = TaxiSchema(typeSchema, listOf())
      whenever(schemaProvider.schema()).thenReturn(taxiSchema)
      val (serviceSchemaGenerator, _) = schemaGeneratorFor(schema)
      val schemaName = argumentCaptor<String>()
      val schemaVersion = argumentCaptor<String>()
      val serviceSchema = argumentCaptor<String>()
      // When
      serviceSchemaGenerator.generateAndPublishService(taxiSchema.versionedType("OrderWindowSummary".fqn()))
      // Then
      verify(schemaStoreClient, times(1)).submitSchema(schemaName.capture(), schemaVersion.capture(), serviceSchema.capture())
      schemaName.firstValue.should.startWith("vyne.casks.OrderWindowSummary@")
      "1.0.0".should.equal(schemaVersion.firstValue)
      """
import OrderWindowSummary
import Symbol
import MaturityDate
import TransactionEventDateTime

namespace vyne.casks {



   @ServiceDiscoveryClient(serviceName = "cask")
   service OrderWindowSummaryCaskService {
      @HttpOperation(method = "GET" , url = "/api/cask/findOneBy/OrderWindowSummary/symbol/{Symbol}")
      operation findOneBySymbol( @PathVariable(name = "symbol") symbol : Symbol ) : OrderWindowSummary
      @HttpOperation(method = "POST" , url = "/api/cask/findMultipleBy/OrderWindowSummary/symbol")
      operation findMultipleBySymbol( @RequestBody symbol : Symbol[] ) : OrderWindowSummary[]
      @HttpOperation(method = "GET" , url = "/api/cask/findSingleBy/OrderWindowSummary/symbol/{id}")
      operation findSingleBySymbol( @PathVariable(name = "id") id : Symbol ) : OrderWindowSummary( Symbol = id )
      @HttpOperation(method = "GET" , url = "/api/cask/OrderWindowSummary/maturityDate/After/{after}")
      operation findByMaturityDateAfter( @PathVariable(name = "after") after : MaturityDate ) : OrderWindowSummary[]( MaturityDate > after )
      @HttpOperation(method = "GET" , url = "/api/cask/OrderWindowSummary/maturityDate/Before/{before}")
      operation findByMaturityDateBefore( @PathVariable(name = "before") before : MaturityDate ) : OrderWindowSummary[]( MaturityDate < before )
      @HttpOperation(method = "GET" , url = "/api/cask/OrderWindowSummary/maturityDate/Between/{start}/{end}")
      operation findByMaturityDateBetween( @PathVariable(name = "start") start : MaturityDate, @PathVariable(name = "end") end : MaturityDate ) : OrderWindowSummary[]( MaturityDate >= start, MaturityDate < end )
      @HttpOperation(method = "GET" , url = "/api/cask/OrderWindowSummary/orderDateTime/After/{after}")
      operation findByOrderDateTimeAfter( @PathVariable(name = "after") after : TransactionEventDateTime ) : OrderWindowSummary[]( TransactionEventDateTime > after )
      @HttpOperation(method = "GET" , url = "/api/cask/OrderWindowSummary/orderDateTime/Before/{before}")
      operation findByOrderDateTimeBefore( @PathVariable(name = "before") before : TransactionEventDateTime ) : OrderWindowSummary[]( TransactionEventDateTime < before )
      @HttpOperation(method = "GET" , url = "/api/cask/OrderWindowSummary/orderDateTime/Between/{start}/{end}")
      operation findByOrderDateTimeBetween( @PathVariable(name = "start") start : TransactionEventDateTime, @PathVariable(name = "end") end : TransactionEventDateTime ) : OrderWindowSummary[]( TransactionEventDateTime >= start, TransactionEventDateTime < end )
   }
}
""".replace("\\s".toRegex(), "").should.equal(serviceSchema.firstValue.replace("\\s".toRegex(), ""))
   }

   @Test
   fun `Cask does not create service if one already exists`() {
      // given
      val typeSchema = lang.taxi.Compiler(schema).compile()
      val versionedType = TaxiSchema(typeSchema, listOf()).versionedType("OrderWindowSummary".fqn())

      val schema: Schema = mock()
      whenever(schemaProvider.schema()).thenReturn(schema)
      whenever(schema.hasService("vyne.casks.OrderWindowSummaryCaskService")).thenReturn(true)
      whenever(schema.versionedType("OrderWindowSummary".fqn())).thenReturn(versionedType)

      val serviceSchemaGenerator = CaskServiceSchemaGenerator(
         schemaProvider,
         caskServiceSchemaWriter,
         listOf(
            FindByFieldIdOperationGenerator(),
            AfterTemporalOperationGenerator(),
            BeforeTemporalOperationGenerator(),
            BetweenTemporalOperationGenerator()))

      // When
      serviceSchemaGenerator.onIngesterInitialised(IngestionInitialisedEvent(this, versionedType))
      serviceSchemaGenerator.onIngesterInitialised(IngestionInitialisedEvent(this, versionedType))

      // Then
      verify(schemaStoreClient, times(0)).submitSchema(any(), any(), any())
   }

}
