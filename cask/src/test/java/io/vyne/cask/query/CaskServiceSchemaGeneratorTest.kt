package io.vyne.cask.query

import com.nhaarman.mockito_kotlin.argumentCaptor
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.times
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import com.winterbe.expekt.should
import io.vyne.cask.query.generators.AfterTemporalOperationGenerator
import io.vyne.cask.query.generators.BeforeTemporalOperationGenerator
import io.vyne.cask.query.generators.BetweenTemporalOperationGenerator
import io.vyne.cask.query.generators.FindByFieldIdOperationGenerator
import io.vyne.schemaStore.SchemaProvider
import io.vyne.schemaStore.SchemaStoreClient
import io.vyne.schemas.fqn
import io.vyne.schemas.taxi.TaxiSchema
import org.junit.Test

class CaskServiceSchemaGeneratorTest {
   val schemaProvider = mock<SchemaProvider>()
   val schemaStoreClient = mock<SchemaStoreClient>()
   val caskServiceSchemaWriter = CaskServiceSchemaWriter(schemaStoreClient)
   private val schema = """
    type alias Price as Decimal
    type alias Symbol as String
    type OrderWindowSummary {
    symbol : Symbol by xpath("/Symbol")
    open : Price by xpath("/Open")
    high : Price by xpath("/High")
    close : Price by xpath("/Close")
    @Before
    @After
    @Between
    maturityDate: Date
    @Before
    @After
    @Between
    orderTime: Instant
}

   """.trimIndent()

   @Test
   fun `Cask generates service schema from valid type schema`() {
      // given
      val typeSchema = lang.taxi.Compiler(schema).compile()
      val taxiSchema = TaxiSchema(typeSchema, listOf())
      whenever(schemaProvider.schema()).thenReturn(taxiSchema)
      val serviceSchemaGenerator = CaskServiceSchemaGenerator(
         schemaProvider,
         caskServiceSchemaWriter,
         listOf(
            FindByFieldIdOperationGenerator(),
            AfterTemporalOperationGenerator(),
            BeforeTemporalOperationGenerator(),
            BetweenTemporalOperationGenerator()))
      val schemaName = argumentCaptor<String>()
      val schemaVersion = argumentCaptor<String>()
      val serviceSchema = argumentCaptor<String>()
      // When
      serviceSchemaGenerator.generate(taxiSchema.versionedType("OrderWindowSummary".fqn()))
      // Then
      verify(schemaStoreClient, times(1)).submitSchema(schemaName.capture(), schemaVersion.capture(), serviceSchema.capture())
      schemaName.firstValue.should.startWith("vyne.casks.OrderWindowSummary-OrderWindowSummary")
      "1.0.0".should.equal(schemaVersion.firstValue)
      """import Symbol import Price namespace vyne.casks {


   @ServiceDiscoveryClient(serviceName = "cask")
   service OrderWindowSummaryCaskService {
      @HttpOperation(method = "GET" , url = "/api/cask/OrderWindowSummary/symbol/{Symbol}")
      operation findBySymbol( @PathVariable(name = "symbol") symbol : Symbol ) : OrderWindowSummary[]
      @HttpOperation(method = "GET" , url = "/api/cask/OrderWindowSummary/open/{Price}")
      operation findByOpen( @PathVariable(name = "open") open : Price ) : OrderWindowSummary[]
      @HttpOperation(method = "GET" , url = "/api/cask/OrderWindowSummary/high/{Price}")
      operation findByHigh( @PathVariable(name = "high") high : Price ) : OrderWindowSummary[]
      @HttpOperation(method = "GET" , url = "/api/cask/OrderWindowSummary/close/{Price}")
      operation findByClose( @PathVariable(name = "close") close : Price ) : OrderWindowSummary[]
      @HttpOperation(method = "GET" , url = "/api/cask/OrderWindowSummary/maturityDate/{lang.taxi.Date}")
      operation findByMaturityDate( @PathVariable(name = "maturityDate") maturityDate : Date ) : OrderWindowSummary[]
      operation findByMaturityDateAfter(  after : Date ) : OrderWindowSummary[]( this:lang.taxi.Date > after )
      operation findByMaturityDateBefore(  before : Date ) : OrderWindowSummary[]( this:lang.taxi.Date < before )
      operation findByMaturityDateBetween(  start : Date,  end : Date ) : OrderWindowSummary[]( this:lang.taxi.Date >= start, this:lang.taxi.Date < end )
      @HttpOperation(method = "GET" , url = "/api/cask/OrderWindowSummary/orderTime/{lang.taxi.Instant}")
      operation findByOrderTime( @PathVariable(name = "orderTime") orderTime : Instant ) : OrderWindowSummary[]
      operation findByOrderTimeAfter(  after : Instant ) : OrderWindowSummary[]( this:lang.taxi.Instant > after )
      operation findByOrderTimeBefore(  before : Instant ) : OrderWindowSummary[]( this:lang.taxi.Instant < before )
      operation findByOrderTimeBetween(  start : Instant,  end : Instant ) : OrderWindowSummary[]( this:lang.taxi.Instant >= start, this:lang.taxi.Instant < end )
   }
}

""".replace("\\s".toRegex(), "").should.equal(serviceSchema.firstValue.replace("\\s".toRegex(), ""))
   }
}
