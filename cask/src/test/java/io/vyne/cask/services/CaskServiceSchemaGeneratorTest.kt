package io.vyne.cask.services

import com.google.common.util.concurrent.MoreExecutors
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.argumentCaptor
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.times
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import com.winterbe.expekt.should
import io.kotest.matchers.shouldBe
import io.vyne.*
import io.vyne.cask.config.CaskConfigRepository
import io.vyne.cask.config.schema
import io.vyne.cask.ddl.views.taxiViews.SchemaBasedViewGenerator
import io.vyne.cask.query.generators.FindAllGenerator
import io.vyne.cask.query.generators.FindBetweenInsertedAtOperationGenerator
import io.vyne.cask.query.generators.FindByFieldIdOperationGenerator
import io.vyne.cask.query.generators.FindByIdGenerators
import io.vyne.cask.query.generators.FindByMultipleGenerator
import io.vyne.cask.query.generators.FindBySingleResultGenerator
import io.vyne.cask.query.generators.InsertedAtGreaterThanOrEqualsToStartLessThanOrEqualsToEndOperationGenerator
import io.vyne.cask.query.generators.InsertedAtGreaterThanStartLessThanEndOperationGenerator
import io.vyne.cask.query.generators.InsertedAtGreaterThanStartLessThanOrEqualsToEndOperationGenerator
import io.vyne.cask.query.generators.OperationAnnotation
import io.vyne.cask.query.generators.OperationGeneratorConfig
import io.vyne.cask.query.generators.VyneQlOperationGenerator
import io.vyne.schema.api.SchemaSet
import io.vyne.schema.consumer.SchemaStore
import io.vyne.schema.publisher.SchemaPublisherTransport
import io.vyne.schemas.SchemaSetChangedEvent
import io.vyne.schemas.fqn
import io.vyne.schemas.taxi.TaxiSchema
import io.vyne.utils.withoutWhitespace
import lang.taxi.types.QualifiedName
import org.junit.Test
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.fail
import org.reactivestreams.Publisher
import reactor.core.publisher.Sinks

class CaskServiceSchemaGeneratorTest {
   val schemaProvider = mock<SchemaStore>()
   val schemaStoreClient = mock<SchemaPublisherTransport>()
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
    @Format ( "yyyy-MM-dd HH:mm:ss.SSSSSSS")
    orderDateTime : TransactionEventDateTime
}

   """.trimIndent()

   private fun schemaGeneratorFor(schema: String, operationGeneratorConfig: OperationGeneratorConfig): Pair<CaskServiceSchemaGenerator, TaxiSchema> {
      val typeSchema = lang.taxi.Compiler(schema).compile()
      val taxiSchema = TaxiSchema(typeSchema, listOf())
      val sources = taxiSchema.sources.map { ParsedSource(it) }
      whenever(schemaProvider.schemaSet).thenReturn(SchemaSet.fromParsed(sources.asParsedPackages(), 1))
      return CaskServiceSchemaGenerator(
         schemaProvider,
         caskServiceSchemaWriter,
         listOf(
            FindByFieldIdOperationGenerator(operationGeneratorConfig),
            FindBySingleResultGenerator(operationGeneratorConfig),
            FindByMultipleGenerator(operationGeneratorConfig),
            FindByIdGenerators(operationGeneratorConfig)),
         listOf(
            FindAllGenerator(),
            VyneQlOperationGenerator(DefaultCaskTypeProvider()),
            FindBetweenInsertedAtOperationGenerator(DefaultCaskTypeProvider()),
            InsertedAtGreaterThanStartLessThanEndOperationGenerator(DefaultCaskTypeProvider()),
            InsertedAtGreaterThanStartLessThanOrEqualsToEndOperationGenerator(DefaultCaskTypeProvider()),
            InsertedAtGreaterThanOrEqualsToStartLessThanOrEqualsToEndOperationGenerator(DefaultCaskTypeProvider())),
         DefaultCaskTypeProvider(),
         "Datasource") to taxiSchema
   }

   @Test
   fun `Cask generate service schema with correct imports`() {
      // given
      val typeSchema = lang.taxi.Compiler(simpleSchema).compile()
      val taxiSchema = TaxiSchema(typeSchema, listOf())
      val sources = taxiSchema.sources.map { ParsedSource(it) }
      whenever(schemaProvider.schemaSet).thenReturn(SchemaSet.fromParsed(sources.asParsedPackages(), 1))
      val (serviceSchemaGenerator, _) = schemaGeneratorFor(simpleSchema, OperationGeneratorConfig(emptyList()))
      val schemas = argumentCaptor<SourcePackage>()
      val removedSchemaIds = argumentCaptor<List<SchemaId>>()

      // When
      serviceSchemaGenerator.generateAndPublishService(CaskTaxiPublicationRequest(taxiSchema.versionedType("OrderWindowSummaryCsv".fqn())))

      // Then
      verify(schemaStoreClient, times(1)).submitPackage(schemas.capture())
      val submittedSchemas = schemas.firstValue.sources
      val schemasByName = submittedSchemas.associateBy { it.name }
      schemasByName["vyne.cask.OrderWindowSummaryCsv"]!!.version.shouldBe("1.1.0")
      """
         namespace vyne.cask {
            type CaskInsertedAt inherits Instant
            type CaskMessageId inherits String
         }
      """.trimIndent()
         .withoutWhitespace()
         .should.equal(schemasByName["vyne.cask.types1"]!!.content.withoutWhitespace())
      """
import OrderWindowSummaryCsv
import vyne.cask.CaskInsertedAt
import Symbol

namespace vyne.cask {
   [[ Generated by Cask.  Source type is OrderWindowSummaryCsv} ]]
   @Generated
   model OrderWindowSummaryCsv inherits OrderWindowSummaryCsv {
      caskInsertedAt : CaskInsertedAt
      caskMessageId : CaskMessageId
   }

   @ServiceDiscoveryClient(serviceName = "cask")
   @Datasource
   service OrderWindowSummaryCsvCaskService {
      @HttpOperation(method = "GET" , url = "/api/cask/findAll/OrderWindowSummaryCsv")
      operation getAll(  ) : OrderWindowSummaryCsv[]
      @HttpOperation(method = "POST", url = "/api/vyneQl")
      vyneQl query vyneQlQueryOrderWindowSummaryCsv(@RequestBody body: vyne.vyneQl.VyneQlQuery):lang.taxi.Array<OrderWindowSummaryCsv> with capabilities {
         filter(==,!=,in,like,>,<,>=,<=)
      }
      @HttpOperation(method = "GET" , url = "/api/cask/OrderWindowSummaryCsv/CaskInsertedAt/Between/{start}/{end}")
      operation findByCaskInsertedAtBetween( @PathVariable(name = "start") start : CaskInsertedAt, @PathVariable(name = "end") end : CaskInsertedAt ) : OrderWindowSummaryCsv[]( vyne.cask.CaskInsertedAt >= start, vyne.cask.CaskInsertedAt < end )
      @HttpOperation(method = "GET" , url = "/api/cask/OrderWindowSummaryCsv/CaskInsertedAt/BetweenGtLt/{start}/{end}")
      operation findByCaskInsertedAtBetweenGtLt( @PathVariable(name = "start") start : CaskInsertedAt, @PathVariable(name = "end") end : CaskInsertedAt ) : OrderWindowSummaryCsv[]( vyne.cask.CaskInsertedAt > start, vyne.cask.CaskInsertedAt < end )
      @HttpOperation(method = "GET" , url = "/api/cask/OrderWindowSummaryCsv/CaskInsertedAt/BetweenGtLte/{start}/{end}")
      operation findByCaskInsertedAtBetweenGtLte( @PathVariable(name = "start") start : CaskInsertedAt, @PathVariable(name = "end") end : CaskInsertedAt ) : OrderWindowSummaryCsv[]( vyne.cask.CaskInsertedAt > start, vyne.cask.CaskInsertedAt <= end )
      @HttpOperation(method = "GET" , url = "/api/cask/OrderWindowSummaryCsv/CaskInsertedAt/BetweenGteLte/{start}/{end}")
      operation findByCaskInsertedAtBetweenGteLte( @PathVariable(name = "start") start : CaskInsertedAt, @PathVariable(name = "end") end : CaskInsertedAt ) : OrderWindowSummaryCsv[]( vyne.cask.CaskInsertedAt >= start, vyne.cask.CaskInsertedAt <= end )
      @HttpOperation(method = "GET" , url = "/api/cask/findSingleBy/OrderWindowSummaryCsv/symbol/{id}")
      operation findSingleBySymbol( @PathVariable(name = "id") id : Symbol ) : OrderWindowSummaryCsv( Symbol == id )
   }
}

      """.trimIndent().withoutWhitespace()
         .should.equal(schemasByName["vyne.cask.OrderWindowSummaryCsv"]!!.content.withoutWhitespace())
   }

   @Test
   fun `Cask generates service schema from valid type schema`() {
      // given
      val typeSchema = lang.taxi.Compiler(schema).compile()
      val taxiSchema = TaxiSchema(typeSchema, listOf())
      val sources = taxiSchema.sources.map { ParsedSource(it) }
      whenever(schemaProvider.schemaSet).thenReturn(SchemaSet.fromParsed(sources.asParsedPackages(), 1))
      val (serviceSchemaGenerator, _) = schemaGeneratorFor(schema, OperationGeneratorConfig(emptyList()))
      val schemas = argumentCaptor<SourcePackage>()
      val removedSchemaIds = argumentCaptor<List<SchemaId>>()

      // When
      serviceSchemaGenerator.generateAndPublishService(CaskTaxiPublicationRequest(taxiSchema.versionedType("OrderWindowSummary".fqn())))
      // Then
      verify(schemaStoreClient, times(1)).submitPackage(schemas.capture())
      val submittedSchemas = schemas.firstValue.sources
      val schemasByName = submittedSchemas.associateBy { it.name }
      """
import OrderWindowSummary
import vyne.cask.CaskInsertedAt
import Symbol
import lang.taxi.Array

namespace vyne.cask {
   [[ Generated by Cask.  Source type is OrderWindowSummary} ]]
   @Generated
   model OrderWindowSummary inherits OrderWindowSummary {
      caskInsertedAt : CaskInsertedAt
      caskMessageId : CaskMessageId
   }

   @ServiceDiscoveryClient(serviceName = "cask")
   @Datasource
   service OrderWindowSummaryCaskService {
      @HttpOperation(method = "GET" , url = "/api/cask/findAll/OrderWindowSummary")
      operation getAll(  ) : OrderWindowSummary[]
      @HttpOperation(method = "POST", url = "/api/vyneQl")
      vyneQl query vyneQlQueryOrderWindowSummary(@RequestBody body: vyne.vyneQl.VyneQlQuery):lang.taxi.Array<OrderWindowSummary> with capabilities {
         filter(==,!=,in,like,>,<,>=,<=)
      }
      @HttpOperation(method = "GET" , url = "/api/cask/OrderWindowSummary/CaskInsertedAt/Between/{start}/{end}")
      operation findByCaskInsertedAtBetween( @PathVariable(name = "start") start : CaskInsertedAt, @PathVariable(name = "end") end : CaskInsertedAt ) : OrderWindowSummary[]( vyne.cask.CaskInsertedAt >= start, vyne.cask.CaskInsertedAt < end )
      @HttpOperation(method = "GET" , url = "/api/cask/OrderWindowSummary/CaskInsertedAt/BetweenGtLt/{start}/{end}")
      operation findByCaskInsertedAtBetweenGtLt( @PathVariable(name = "start") start : CaskInsertedAt, @PathVariable(name = "end") end : CaskInsertedAt ) : OrderWindowSummary[]( vyne.cask.CaskInsertedAt > start, vyne.cask.CaskInsertedAt < end )
      @HttpOperation(method = "GET" , url = "/api/cask/OrderWindowSummary/CaskInsertedAt/BetweenGtLte/{start}/{end}")
      operation findByCaskInsertedAtBetweenGtLte( @PathVariable(name = "start") start : CaskInsertedAt, @PathVariable(name = "end") end : CaskInsertedAt ) : OrderWindowSummary[]( vyne.cask.CaskInsertedAt > start, vyne.cask.CaskInsertedAt <= end )
      @HttpOperation(method = "GET" , url = "/api/cask/OrderWindowSummary/CaskInsertedAt/BetweenGteLte/{start}/{end}")
      operation findByCaskInsertedAtBetweenGteLte( @PathVariable(name = "start") start : CaskInsertedAt, @PathVariable(name = "end") end : CaskInsertedAt ) : OrderWindowSummary[]( vyne.cask.CaskInsertedAt >= start, vyne.cask.CaskInsertedAt <= end )
      @HttpOperation(method = "GET" , url = "/api/cask/OrderWindowSummary/symbol/{Symbol}")
      operation findBySymbol( @PathVariable(name = "symbol") symbol : Symbol ) : OrderWindowSummary[]( Symbol == symbol )
      @HttpOperation(method = "GET" , url = "/api/cask/findOneBy/OrderWindowSummary/symbol/{Symbol}")
      operation findOneBySymbol( @PathVariable(name = "symbol") symbol : Symbol ) : OrderWindowSummary
      @HttpOperation(method = "POST" , url = "/api/cask/findMultipleBy/OrderWindowSummary/symbol")
      operation findMultipleBySymbol( @RequestBody symbol : Symbol[] ) : OrderWindowSummary[]
      @HttpOperation(method = "GET" , url = "/api/cask/findSingleBy/OrderWindowSummary/symbol/{id}")
      operation findSingleBySymbol( @PathVariable(name = "id") id : Symbol ) : OrderWindowSummary( Symbol == id )
   }
}
""".trimIndent()
         .withoutWhitespace()
         .should.equal(schemasByName["vyne.cask.OrderWindowSummary"]!!.content.withoutWhitespace())

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
            "namespace vyne.cask\nservice OrderWindowSummaryCaskService {}"))
      val sources = taxiSchema.sources.map { ParsedSource(it) } + caskServiceSource
      whenever(schemaProvider.schemaSet).thenReturn(SchemaSet.fromParsed(sources.asParsedPackages(), 1))

      // When
      //     serviceSchemaGenerator.onIngesterInitialised(IngestionInitialisedEvent(this, versionedType))
      //     serviceSchemaGenerator.onIngesterInitialised(IngestionInitialisedEvent(this, versionedType))

      // Then
      verify(schemaStoreClient, times(0)).submitPackage(any())
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
      whenever(schemaProvider.schemaSet).thenReturn(SchemaSet.fromParsed(sources.asParsedPackages(), 1))
      val config = OperationGeneratorConfig(
         listOf(
            OperationGeneratorConfig.OperationConfigDefinition("String", OperationAnnotation.Id),
            OperationGeneratorConfig.OperationConfigDefinition("LogDate", OperationAnnotation.Between),
            OperationGeneratorConfig.OperationConfigDefinition("Name", OperationAnnotation.After)))
      val (serviceSchemaGenerator, _) = schemaGeneratorFor(simpleSchema, config)
      val schemas = argumentCaptor<SourcePackage>()
      val removedSchemaIds = argumentCaptor<List<SchemaId>>()

      // When
      serviceSchemaGenerator.generateAndPublishService(CaskTaxiPublicationRequest(taxiSchema.versionedType("Simple".fqn())))
      // Then
      verify(schemaStoreClient, times(1)).submitPackage(schemas.capture())
      val submittedSchemas = schemas.firstValue.sources
      val schemasByName = submittedSchemas.associateBy { it.name }
      submittedSchemas.size.should.equal(4)
      """
import Simple
import vyne.cask.CaskInsertedAt

namespace vyne.cask {
   [[ Generated by Cask.  Source type is Simple} ]]
   @Generated
   model Simple inherits Simple {
      caskInsertedAt : CaskInsertedAt
      caskMessageId : CaskMessageId
   }

   @ServiceDiscoveryClient(serviceName = "cask")
   @Datasource
   service SimpleCaskService {
      @HttpOperation(method = "GET" , url = "/api/cask/findAll/Simple")
      operation getAll(  ) : Simple[]
      @HttpOperation(method = "POST", url = "/api/vyneQl")
      vyneQl query vyneQlQuerySimple(@RequestBody body: vyne.vyneQl.VyneQlQuery):lang.taxi.Array<Simple> with capabilities {
         filter(==,!=,in,like,>,<,>=,<=)
      }
      @HttpOperation(method = "GET" , url = "/api/cask/Simple/CaskInsertedAt/Between/{start}/{end}")
      operation findByCaskInsertedAtBetween( @PathVariable(name = "start") start : CaskInsertedAt, @PathVariable(name = "end") end : CaskInsertedAt ) : Simple[]( vyne.cask.CaskInsertedAt >= start, vyne.cask.CaskInsertedAt < end )
      @HttpOperation(method = "GET" , url = "/api/cask/Simple/CaskInsertedAt/BetweenGtLt/{start}/{end}")
      operation findByCaskInsertedAtBetweenGtLt( @PathVariable(name = "start") start : CaskInsertedAt, @PathVariable(name = "end") end : CaskInsertedAt ) : Simple[]( vyne.cask.CaskInsertedAt > start, vyne.cask.CaskInsertedAt < end )
      @HttpOperation(method = "GET" , url = "/api/cask/Simple/CaskInsertedAt/BetweenGtLte/{start}/{end}")
      operation findByCaskInsertedAtBetweenGtLte( @PathVariable(name = "start") start : CaskInsertedAt, @PathVariable(name = "end") end : CaskInsertedAt ) : Simple[]( vyne.cask.CaskInsertedAt > start, vyne.cask.CaskInsertedAt <= end )
      @HttpOperation(method = "GET" , url = "/api/cask/Simple/CaskInsertedAt/BetweenGteLte/{start}/{end}")
      operation findByCaskInsertedAtBetweenGteLte( @PathVariable(name = "start") start : CaskInsertedAt, @PathVariable(name = "end") end : CaskInsertedAt ) : Simple[]( vyne.cask.CaskInsertedAt >= start, vyne.cask.CaskInsertedAt <= end )
   }
}
      """.trimIndent()
         .trimMargin()
         .withoutWhitespace()
         .should
         .equal(schemasByName["vyne.cask.Simple"]!!.content.withoutWhitespace())
   }

   @Test
   fun `Cask can generate operations for taxi View config`() {
      val simpleSchema = """
         type Id inherits String
         type Name inherits String
         type LogDate inherits Instant
         type LogDatePlus inherits LogDate

         model Simple {
            id : Id
            name: Name
            logDatePlus: LogDatePlus
         }

         view SimpleView with query {
            find { Simple[] } as {
               id: Simple::Id
            }
         }
      """.trimIndent()
      // given
      val typeSchema = lang.taxi.Compiler(simpleSchema).compile()
      val taxiSchema = TaxiSchema(typeSchema, listOf(VersionedSource.sourceOnly(simpleSchema).asPackage()))
      val sources = taxiSchema.sources.map { ParsedSource(it) }
      val schemaStore = object : SchemaStore {
          override val schemaSet: SchemaSet
              get() {
                  return SchemaSet.fromParsed(sources.asParsedPackages(), 1)
              }

         override val generation: Int
            get() = 1

         val schemaSetChangedEventSink = Sinks.many().replay().latest<SchemaSetChangedEvent>()
         override val schemaChanged: Publisher<SchemaSetChangedEvent>
            get() = schemaSetChangedEventSink.asFlux()

      }
      val configRepo = mock<CaskConfigRepository>()
      val viewGenerator = SchemaBasedViewGenerator(configRepo, schemaStore)
      val viewCaskConfig = viewGenerator.generateCaskConfig(taxiSchema.document.views.first())
      val viewModel = viewGenerator.typeFromView(taxiSchema.document.views.first())

      whenever(schemaProvider.schemaSet).thenReturn(SchemaSet.fromParsed(sources.asParsedPackages(), 1))
      val config = OperationGeneratorConfig(
         listOf(
            OperationGeneratorConfig.OperationConfigDefinition("String", OperationAnnotation.Id),
            OperationGeneratorConfig.OperationConfigDefinition("LogDate", OperationAnnotation.Between),
            OperationGeneratorConfig.OperationConfigDefinition("Name", OperationAnnotation.After)))
      val (serviceSchemaGenerator, _) = schemaGeneratorFor(simpleSchema, config)
      val schemas = argumentCaptor<SourcePackage>()
      val removedSchemaIds = argumentCaptor<List<SchemaId>>()

      val caskSchema = viewCaskConfig.schema(TaxiSchema.from(simpleSchema))
      val type = caskSchema.versionedType(viewCaskConfig.qualifiedTypeName.fqn())
      // When
      serviceSchemaGenerator.generateAndPublishService(CaskTaxiPublicationRequest(type, excludedCaskServices = setOf(QualifiedName.from("Service1"), QualifiedName.from("Service2"))))
      // Then
      verify(schemaStoreClient, times(1)).submitPackage(schemas.capture())
      val submittedSchemas = schemas.firstValue.sources
      val schemasByName = submittedSchemas.associateBy { it.name }
      val actual = schemasByName["vyne.cask.SimpleView"]!!.content
      val expected = """
import SimpleView
import vyne.cask.CaskInsertedAt

namespace vyne.cask {
   [[ Generated by Cask.  Source type is SimpleView} ]]
   @Generated
   model SimpleView inherits SimpleView {
      caskInsertedAt : CaskInsertedAt
      caskMessageId : CaskMessageId
   }

   @ServiceDiscoveryClient(serviceName = "cask")
   @Datasource(exclude = "[[Service1, Service2]]")
   service SimpleViewCaskService {
      @HttpOperation(method = "GET" , url = "/api/cask/findAll/SimpleView")
      operation getAll(  ) : SimpleView[]
      @HttpOperation(method = "POST", url = "/api/vyneQl")
      vyneQl query vyneQlQuerySimpleView(@RequestBody body: vyne.vyneQl.VyneQlQuery):lang.taxi.Array<SimpleView> with capabilities {
         filter(==,!=,in,like,>,<,>=,<=)
      }
      @HttpOperation(method = "GET" , url = "/api/cask/SimpleView/CaskInsertedAt/Between/{start}/{end}")
      operation findByCaskInsertedAtBetween( @PathVariable(name = "start") start : CaskInsertedAt, @PathVariable(name = "end") end : CaskInsertedAt ) : SimpleView[]( vyne.cask.CaskInsertedAt >= start, vyne.cask.CaskInsertedAt < end )
      @HttpOperation(method = "GET" , url = "/api/cask/SimpleView/CaskInsertedAt/BetweenGtLt/{start}/{end}")
      operation findByCaskInsertedAtBetweenGtLt( @PathVariable(name = "start") start : CaskInsertedAt, @PathVariable(name = "end") end : CaskInsertedAt ) : SimpleView[]( vyne.cask.CaskInsertedAt > start, vyne.cask.CaskInsertedAt < end )
      @HttpOperation(method = "GET" , url = "/api/cask/SimpleView/CaskInsertedAt/BetweenGtLte/{start}/{end}")
      operation findByCaskInsertedAtBetweenGtLte( @PathVariable(name = "start") start : CaskInsertedAt, @PathVariable(name = "end") end : CaskInsertedAt ) : SimpleView[]( vyne.cask.CaskInsertedAt > start, vyne.cask.CaskInsertedAt <= end )
      @HttpOperation(method = "GET" , url = "/api/cask/SimpleView/CaskInsertedAt/BetweenGteLte/{start}/{end}")
      operation findByCaskInsertedAtBetweenGteLte( @PathVariable(name = "start") start : CaskInsertedAt, @PathVariable(name = "end") end : CaskInsertedAt ) : SimpleView[]( vyne.cask.CaskInsertedAt >= start, vyne.cask.CaskInsertedAt <= end )
   }
}

      """.trimIndent()
         .trimMargin()
         .withoutWhitespace()
         .trim()

      actual.trimIndent().trimMargin().withoutWhitespace().trim()
         .shouldBe(expected)
   }

   @Test
   fun `When a cask is deleted relevant types are unpublished`() {
      // first publish.
      val typeSchema = lang.taxi.Compiler(simpleSchema).compile()
      val taxiSchema = TaxiSchema(typeSchema, listOf())
      val sources = taxiSchema.sources.map { ParsedSource(it) }
      whenever(schemaProvider.schemaSet).thenReturn(SchemaSet.fromParsed(sources.asParsedPackages(), 1))
      val (serviceSchemaGenerator, _) = schemaGeneratorFor(simpleSchema, OperationGeneratorConfig(emptyList()))
      val schemas = argumentCaptor<SourcePackage>()
      val removedSchemaIds = argumentCaptor<List<SchemaId>>()
      serviceSchemaGenerator.generateAndPublishService(CaskTaxiPublicationRequest(taxiSchema.versionedType("OrderWindowSummaryCsv".fqn())))
      caskServiceSchemaWriter.clearFromCaskSchema(listOf(QualifiedName.from("OrderWindowSummaryCsv")))

      // two schema publications, one for the initial publication another one for delete.
      verify(schemaStoreClient, times(2)).submitPackage(schemas.capture())
      schemas.firstValue.sources.map { it.name }.should.contain("vyne.cask.OrderWindowSummaryCsv")
      schemas.lastValue.sources.map { it.name }.should.not.contain("vyne.cask.OrderWindowSummaryCsv")
   }

   private val simpleSchema = """[[
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
          @Format( 'yyyy-MM-dd hh-a' )
          orderDate : DateTime by column(1)
          @Id
          symbol : Symbol by column(2)
          open : Price by column(3)
          close : Price by column(4)
      }""".trimIndent()
}
