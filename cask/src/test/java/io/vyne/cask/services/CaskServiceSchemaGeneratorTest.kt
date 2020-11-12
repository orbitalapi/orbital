package io.vyne.cask.services

import com.google.common.util.concurrent.MoreExecutors
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.argumentCaptor
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.times
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import com.winterbe.expekt.should
import io.vyne.ParsedSource
import io.vyne.VersionedSource
import io.vyne.cask.query.generators.*
import io.vyne.schemaStore.SchemaSet
import io.vyne.schemaStore.SchemaStore
import io.vyne.schemaStore.SchemaStoreClient
import io.vyne.schemas.fqn
import io.vyne.schemas.taxi.TaxiSchema
import lang.taxi.types.PrimitiveType
import org.junit.Test

class CaskServiceSchemaGeneratorTest {
   val schemaProvider = mock<SchemaStore>()
   val schemaStoreClient = mock<SchemaStoreClient>()
   val caskServiceSchemaWriter = CaskServiceSchemaWriter(
      schemaPublisher = schemaStoreClient,
      defaultCaskTypeProvider = DefaultCaskTypeProvider(),
      caskDefinitionPublicationExecutor = MoreExecutors.newDirectExecutorService())
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
      val (serviceSchemaGenerator,taxiSchema) = schemaGeneratorFor(schema, OperationGeneratorConfig(emptyList()))

      // When
      val generated = serviceSchemaGenerator.generateSchema(CaskTaxiPublicationRequest(taxiSchema.versionedType("Trade".fqn())))
      val operation = generated.services.first().operation("findByTradeDateBefore")
      operation.parameters.first().type.qualifiedName.should.equal(PrimitiveType.INSTANT.qualifiedName)
   }

   private fun schemaGeneratorFor(schema: String, operationGeneratorConfig: OperationGeneratorConfig): Pair<CaskServiceSchemaGenerator,TaxiSchema> {
      val typeSchema = lang.taxi.Compiler(schema).compile()
      val taxiSchema = TaxiSchema(typeSchema, listOf())
      val sources = taxiSchema.sources.map { ParsedSource(it) }
      whenever(schemaProvider.schemaSet()).thenReturn(SchemaSet.fromParsed(sources, 1))
      return CaskServiceSchemaGenerator(
         schemaProvider,
         caskServiceSchemaWriter,
         listOf(
            FindByFieldIdOperationGenerator(operationGeneratorConfig),
            AfterTemporalOperationGenerator(operationGeneratorConfig),
            BeforeTemporalOperationGenerator(operationGeneratorConfig),
            BetweenTemporalOperationGenerator(operationGeneratorConfig),
            FindBySingleResultGenerator(operationGeneratorConfig),
            FindByMultipleGenerator(operationGeneratorConfig),
            FindByIdGenerators(operationGeneratorConfig),
            GreaterThanStartLessThanEndOperationGenerator(operationGeneratorConfig),
            GreaterThanStartLessThanOrEqualsToEndOperationGenerator(operationGeneratorConfig),
            GreaterThanOrEqualsToStartLessThanOrEqualsToEndOperationGenerator(operationGeneratorConfig)),
         listOf(
            FindAllGenerator(),
            FindBetweenInsertedAtOperationGenerator(DefaultCaskTypeProvider()),
            InsertedAtGreaterThanStartLessThanEndOperationGenerator(DefaultCaskTypeProvider()),
            InsertedAtGreaterThanStartLessThanOrEqualsToEndOperationGenerator(DefaultCaskTypeProvider()),
            InsertedAtGreaterThanOrEqualsToStartLessThanOrEqualsToEndOperationGenerator(DefaultCaskTypeProvider())),
      "Datasource") to taxiSchema
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
      val sources = taxiSchema.sources.map { ParsedSource(it) }
      whenever(schemaProvider.schemaSet()).thenReturn(SchemaSet.fromParsed(sources, 1))
      val (serviceSchemaGenerator, _) = schemaGeneratorFor(simpleSchema, OperationGeneratorConfig(emptyList()))
      val schemas = argumentCaptor<List<VersionedSource>>()

      // When
      serviceSchemaGenerator.generateAndPublishService(CaskTaxiPublicationRequest(taxiSchema.versionedType("OrderWindowSummaryCsv".fqn())))

      // Then
      verify(schemaStoreClient, times(1)).submitSchemas(schemas.capture())
      val submittedSchemas = schemas.firstValue
      submittedSchemas.size.should.equal(2)
      submittedSchemas[1].name.should.equal("vyne.casks.OrderWindowSummaryCsv")
      submittedSchemas[1].version.should.equal("1.0.1")
      """
import OrderWindowSummaryCsv
import vyne.cask.CaskInsertedAt
import Symbol

namespace vyne.casks {



   @ServiceDiscoveryClient(serviceName = "cask")
   @Datasource
   service OrderWindowSummaryCsvCaskService {
      @HttpOperation(method = "GET" , url = "/api/cask/findAll/OrderWindowSummaryCsv")
      operation findAll(  ) : OrderWindowSummaryCsv[]
      @HttpOperation(method = "GET" , url = "/api/cask/OrderWindowSummaryCsv/CaskInsertedAt/Between/{start}/{end}")
      operation findByCaskInsertedAtBetween( @PathVariable(name = "start") start : vyne.cask.CaskInsertedAt, @PathVariable(name = "end") end : vyne.cask.CaskInsertedAt ) : OrderWindowSummaryCsv[]( vyne.cask.CaskInsertedAt >= start, vyne.cask.CaskInsertedAt < end )
      @HttpOperation(method = "GET" , url = "/api/cask/OrderWindowSummaryCsv/CaskInsertedAt/BetweenGtLt/{start}/{end}")
      operation findByCaskInsertedAtBetweenGtLt( @PathVariable(name = "start") start : vyne.cask.CaskInsertedAt, @PathVariable(name = "end") end : vyne.cask.CaskInsertedAt ) : OrderWindowSummaryCsv[]( vyne.cask.CaskInsertedAt > start, vyne.cask.CaskInsertedAt < end )
      @HttpOperation(method = "GET" , url = "/api/cask/OrderWindowSummaryCsv/CaskInsertedAt/BetweenGtLte/{start}/{end}")
      operation findByCaskInsertedAtBetweenGtLte( @PathVariable(name = "start") start : vyne.cask.CaskInsertedAt, @PathVariable(name = "end") end : vyne.cask.CaskInsertedAt ) : OrderWindowSummaryCsv[]( vyne.cask.CaskInsertedAt > start, vyne.cask.CaskInsertedAt <= end )
      @HttpOperation(method = "GET" , url = "/api/cask/OrderWindowSummaryCsv/CaskInsertedAt/BetweenGteLte/{start}/{end}")
      operation findByCaskInsertedAtBetweenGteLte( @PathVariable(name = "start") start : vyne.cask.CaskInsertedAt, @PathVariable(name = "end") end : vyne.cask.CaskInsertedAt ) : OrderWindowSummaryCsv[]( vyne.cask.CaskInsertedAt >= start, vyne.cask.CaskInsertedAt <= end )
      @HttpOperation(method = "GET" , url = "/api/cask/findSingleBy/OrderWindowSummaryCsv/symbol/{id}")
      operation findSingleBySymbol( @PathVariable(name = "id") id : Symbol ) : OrderWindowSummaryCsv( Symbol = id )
   }
}

""".replace("\\s".toRegex(), "").should.equal(submittedSchemas[1].content.replace("\\s".toRegex(), ""))
   }

   @Test
   fun `Cask generates service schema from valid type schema`() {
      // given
      val typeSchema = lang.taxi.Compiler(schema).compile()
      val taxiSchema = TaxiSchema(typeSchema, listOf())
      val sources = taxiSchema.sources.map { ParsedSource(it) }
      whenever(schemaProvider.schemaSet()).thenReturn(SchemaSet.fromParsed(sources, 1))
      val (serviceSchemaGenerator, _) = schemaGeneratorFor(schema, OperationGeneratorConfig(emptyList()))
      val schemas = argumentCaptor<List<VersionedSource>>()

      // When
      serviceSchemaGenerator.generateAndPublishService(CaskTaxiPublicationRequest(taxiSchema.versionedType("OrderWindowSummary".fqn())))
      // Then
      verify(schemaStoreClient, times(1)).submitSchemas(schemas.capture())
      val submittedSchemas = schemas.firstValue
      submittedSchemas.size.should.equal(2)
      submittedSchemas[1].name.should.equal("vyne.casks.OrderWindowSummary")
      submittedSchemas[1].version.should.equal("1.0.1")
      """
import OrderWindowSummary
import vyne.cask.CaskInsertedAt
import Symbol
import MaturityDate
import TransactionEventDateTime

namespace vyne.casks {


   @ServiceDiscoveryClient(serviceName = "cask")
   @Datasource
   service OrderWindowSummaryCaskService {
      @HttpOperation(method = "GET" , url = "/api/cask/findAll/OrderWindowSummary")
      operation findAll(  ) : OrderWindowSummary[]
      @HttpOperation(method = "GET" , url = "/api/cask/OrderWindowSummary/CaskInsertedAt/Between/{start}/{end}")
      operation findByCaskInsertedAtBetween( @PathVariable(name = "start") start : vyne.cask.CaskInsertedAt, @PathVariable(name = "end") end : vyne.cask.CaskInsertedAt ) : OrderWindowSummary[]( vyne.cask.CaskInsertedAt >= start, vyne.cask.CaskInsertedAt < end )
      @HttpOperation(method = "GET" , url = "/api/cask/OrderWindowSummary/CaskInsertedAt/BetweenGtLt/{start}/{end}")
      operation findByCaskInsertedAtBetweenGtLt( @PathVariable(name = "start") start : vyne.cask.CaskInsertedAt, @PathVariable(name = "end") end : vyne.cask.CaskInsertedAt ) : OrderWindowSummary[]( vyne.cask.CaskInsertedAt > start, vyne.cask.CaskInsertedAt < end )
      @HttpOperation(method = "GET" , url = "/api/cask/OrderWindowSummary/CaskInsertedAt/BetweenGtLte/{start}/{end}")
      operation findByCaskInsertedAtBetweenGtLte( @PathVariable(name = "start") start : vyne.cask.CaskInsertedAt, @PathVariable(name = "end") end : vyne.cask.CaskInsertedAt ) : OrderWindowSummary[]( vyne.cask.CaskInsertedAt > start, vyne.cask.CaskInsertedAt <= end )
      @HttpOperation(method = "GET" , url = "/api/cask/OrderWindowSummary/CaskInsertedAt/BetweenGteLte/{start}/{end}")
      operation findByCaskInsertedAtBetweenGteLte( @PathVariable(name = "start") start : vyne.cask.CaskInsertedAt, @PathVariable(name = "end") end : vyne.cask.CaskInsertedAt ) : OrderWindowSummary[]( vyne.cask.CaskInsertedAt >= start, vyne.cask.CaskInsertedAt <= end )
      @HttpOperation(method = "GET" , url = "/api/cask/OrderWindowSummary/symbol/{Symbol}")
      operation findBySymbol( @PathVariable(name = "symbol") symbol : Symbol ) : OrderWindowSummary[]( Symbol = symbol )
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
      @HttpOperation(method = "GET" , url = "/api/cask/OrderWindowSummary/maturityDate/BetweenGtLt/{start}/{end}")
      operation findByMaturityDateBetweenGtLt( @PathVariable(name = "start") start : MaturityDate, @PathVariable(name = "end") end : MaturityDate ) : OrderWindowSummary[]( MaturityDate > start, MaturityDate < end )
      @HttpOperation(method = "GET" , url = "/api/cask/OrderWindowSummary/maturityDate/BetweenGtLte/{start}/{end}")
      operation findByMaturityDateBetweenGtLte( @PathVariable(name = "start") start : MaturityDate, @PathVariable(name = "end") end : MaturityDate ) : OrderWindowSummary[]( MaturityDate > start, MaturityDate <= end )
      @HttpOperation(method = "GET" , url = "/api/cask/OrderWindowSummary/maturityDate/BetweenGteLte/{start}/{end}")
      operation findByMaturityDateBetweenGteLte( @PathVariable(name = "start") start : MaturityDate, @PathVariable(name = "end") end : MaturityDate ) : OrderWindowSummary[]( MaturityDate >= start, MaturityDate <= end )
      @HttpOperation(method = "GET" , url = "/api/cask/OrderWindowSummary/orderDateTime/After/{after}")
      operation findByOrderDateTimeAfter( @PathVariable(name = "after") after : TransactionEventDateTime ) : OrderWindowSummary[]( TransactionEventDateTime > after )
      @HttpOperation(method = "GET" , url = "/api/cask/OrderWindowSummary/orderDateTime/Before/{before}")
      operation findByOrderDateTimeBefore( @PathVariable(name = "before") before : TransactionEventDateTime ) : OrderWindowSummary[]( TransactionEventDateTime < before )
      @HttpOperation(method = "GET" , url = "/api/cask/OrderWindowSummary/orderDateTime/Between/{start}/{end}")
      operation findByOrderDateTimeBetween( @PathVariable(name = "start") start : TransactionEventDateTime, @PathVariable(name = "end") end : TransactionEventDateTime ) : OrderWindowSummary[]( TransactionEventDateTime >= start, TransactionEventDateTime < end )
      @HttpOperation(method = "GET" , url = "/api/cask/OrderWindowSummary/orderDateTime/BetweenGtLt/{start}/{end}")
      operation findByOrderDateTimeBetweenGtLt( @PathVariable(name = "start") start : TransactionEventDateTime, @PathVariable(name = "end") end : TransactionEventDateTime ) : OrderWindowSummary[]( TransactionEventDateTime > start, TransactionEventDateTime < end )
      @HttpOperation(method = "GET" , url = "/api/cask/OrderWindowSummary/orderDateTime/BetweenGtLte/{start}/{end}")
      operation findByOrderDateTimeBetweenGtLte( @PathVariable(name = "start") start : TransactionEventDateTime, @PathVariable(name = "end") end : TransactionEventDateTime ) : OrderWindowSummary[]( TransactionEventDateTime > start, TransactionEventDateTime <= end )
      @HttpOperation(method = "GET" , url = "/api/cask/OrderWindowSummary/orderDateTime/BetweenGteLte/{start}/{end}")
      operation findByOrderDateTimeBetweenGteLte( @PathVariable(name = "start") start : TransactionEventDateTime, @PathVariable(name = "end") end : TransactionEventDateTime ) : OrderWindowSummary[]( TransactionEventDateTime >= start, TransactionEventDateTime <= end )
   }
}
""".replace("\\s".toRegex(), "").should.equal(submittedSchemas[1].content.replace("\\s".toRegex(), ""))
   }

   @Test
   fun `Cask does not create service if one already exists`() {
      // given
      val typeSchema = lang.taxi.Compiler(schema).compile()
      val taxiSchema = TaxiSchema(typeSchema, listOf())
      val versionedType = taxiSchema.versionedType("OrderWindowSummary".fqn())

      val (_, _) = schemaGeneratorFor(schema, OperationGeneratorConfig(emptyList()))
      val caskServiceSource = ParsedSource(
         VersionedSource(
            CaskServiceSchemaGenerator.caskServiceSchemaName(versionedType),
            "1.0.1",
         "namespace vyne.casks\nservice OrderWindowSummaryCaskService {}"))
      val sources = taxiSchema.sources.map { ParsedSource(it) } + caskServiceSource
      whenever(schemaProvider.schemaSet()).thenReturn(SchemaSet.fromParsed(sources, 1))

      // When
 //     serviceSchemaGenerator.onIngesterInitialised(IngestionInitialisedEvent(this, versionedType))
 //     serviceSchemaGenerator.onIngesterInitialised(IngestionInitialisedEvent(this, versionedType))

      // Then
      verify(schemaStoreClient, times(0)).submitSchemas(any())
   }

   @Test
   fun `Cask can generate operations from the definitions in app config`() {
      val simpleSchema = """
         type Name inherits String
         type LogDate inherits Instant
         type LogDatePlus inherits LogDate

         type Simple {
            id : String
            name: Name
            logDatePlus: LogDatePlus
         }
      """.trimIndent()
      // given
      val typeSchema = lang.taxi.Compiler(simpleSchema).compile()
      val taxiSchema = TaxiSchema(typeSchema, listOf())
      val sources = taxiSchema.sources.map { ParsedSource(it) }
      whenever(schemaProvider.schemaSet()).thenReturn(SchemaSet.fromParsed(sources, 1))
      val config = OperationGeneratorConfig(
         listOf(
            OperationGeneratorConfig.OperationConfigDefinition("String", OperationAnnotation.Id),
            OperationGeneratorConfig.OperationConfigDefinition("LogDate", OperationAnnotation.Between),
            OperationGeneratorConfig.OperationConfigDefinition("Name", OperationAnnotation.After)))
      val (serviceSchemaGenerator, _) = schemaGeneratorFor(simpleSchema, config)
      val schemas = argumentCaptor<List<VersionedSource>>()

      // When
      serviceSchemaGenerator.generateAndPublishService(CaskTaxiPublicationRequest(taxiSchema.versionedType("Simple".fqn())))
      // Then
      verify(schemaStoreClient, times(1)).submitSchemas(schemas.capture())
      val submittedSchemas = schemas.firstValue
      submittedSchemas.size.should.equal(2)
      submittedSchemas[1].name.should.equal("vyne.casks.Simple")
      submittedSchemas[1].version.should.equal("1.0.1")
      """
import Simple
import vyne.cask.CaskInsertedAt
import LogDatePlus

namespace vyne.casks {


   @ServiceDiscoveryClient(serviceName = "cask")
   @Datasource
   service SimpleCaskService {
      @HttpOperation(method = "GET" , url = "/api/cask/findAll/Simple")
      operation findAll(  ) : Simple[]
      @HttpOperation(method = "GET" , url = "/api/cask/Simple/CaskInsertedAt/Between/{start}/{end}")
      operation findByCaskInsertedAtBetween( @PathVariable(name = "start") start : vyne.cask.CaskInsertedAt, @PathVariable(name = "end") end : vyne.cask.CaskInsertedAt ) : Simple[]( vyne.cask.CaskInsertedAt >= start, vyne.cask.CaskInsertedAt < end )
      @HttpOperation(method = "GET" , url = "/api/cask/Simple/CaskInsertedAt/BetweenGtLt/{start}/{end}")
      operation findByCaskInsertedAtBetweenGtLt( @PathVariable(name = "start") start : vyne.cask.CaskInsertedAt, @PathVariable(name = "end") end : vyne.cask.CaskInsertedAt ) : Simple[]( vyne.cask.CaskInsertedAt > start, vyne.cask.CaskInsertedAt < end )
      @HttpOperation(method = "GET" , url = "/api/cask/Simple/CaskInsertedAt/BetweenGtLte/{start}/{end}")
      operation findByCaskInsertedAtBetweenGtLte( @PathVariable(name = "start") start : vyne.cask.CaskInsertedAt, @PathVariable(name = "end") end : vyne.cask.CaskInsertedAt ) : Simple[]( vyne.cask.CaskInsertedAt > start, vyne.cask.CaskInsertedAt <= end )
      @HttpOperation(method = "GET" , url = "/api/cask/Simple/CaskInsertedAt/BetweenGteLte/{start}/{end}")
      operation findByCaskInsertedAtBetweenGteLte( @PathVariable(name = "start") start : vyne.cask.CaskInsertedAt, @PathVariable(name = "end") end : vyne.cask.CaskInsertedAt ) : Simple[]( vyne.cask.CaskInsertedAt >= start, vyne.cask.CaskInsertedAt <= end )
      @HttpOperation(method = "GET" , url = "/api/cask/Simple/logDatePlus/Between/{start}/{end}")
      operation findByLogDatePlusBetween( @PathVariable(name = "start") start : LogDatePlus, @PathVariable(name = "end") end : LogDatePlus ) : Simple[]( LogDatePlus >= start, LogDatePlus < end )
      @HttpOperation(method = "GET" , url = "/api/cask/Simple/logDatePlus/BetweenGtLt/{start}/{end}")
      operation findByLogDatePlusBetweenGtLt( @PathVariable(name = "start") start : LogDatePlus, @PathVariable(name = "end") end : LogDatePlus ) : Simple[]( LogDatePlus > start, LogDatePlus < end )
      @HttpOperation(method = "GET" , url = "/api/cask/Simple/logDatePlus/BetweenGtLte/{start}/{end}")
      operation findByLogDatePlusBetweenGtLte( @PathVariable(name = "start") start : LogDatePlus, @PathVariable(name = "end") end : LogDatePlus ) : Simple[]( LogDatePlus > start, LogDatePlus <= end )
      @HttpOperation(method = "GET" , url = "/api/cask/Simple/logDatePlus/BetweenGteLte/{start}/{end}")
      operation findByLogDatePlusBetweenGteLte( @PathVariable(name = "start") start : LogDatePlus, @PathVariable(name = "end") end : LogDatePlus ) : Simple[]( LogDatePlus >= start, LogDatePlus <= end )
   }
}
      """.trimMargin().replace("\\s".toRegex(), "").should.equal(submittedSchemas[1].content.replace("\\s".toRegex(), ""))
   }
}
