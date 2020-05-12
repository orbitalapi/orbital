package io.vyne.cask.query

import com.nhaarman.mockito_kotlin.argumentCaptor
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.times
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import com.winterbe.expekt.should
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
    // Added column
    high : Price by xpath("/High")
    // Changed column
    close : Price by xpath("/Close")
}

   """.trimIndent()

   @Test
   fun `Cask generates service schema from valid type schema`() {
      // given
      val typeSchema = lang.taxi.Compiler(schema).compile()
      val taxiSchema = TaxiSchema(typeSchema, listOf())
      whenever(schemaProvider.schema()).thenReturn(taxiSchema)
      val serviceSchemaGenerator = CaskServiceSchemaGenerator(schemaProvider, caskServiceSchemaWriter)
      val schemaName = argumentCaptor<String>()
      val schemaVersion = argumentCaptor<String>()
      val serviceSchema = argumentCaptor<String>()
      // When
      serviceSchemaGenerator.generate(taxiSchema.versionedType("OrderWindowSummary".fqn()))
      // Then
      verify(schemaStoreClient, times(1)).submitSchema(schemaName.capture(), schemaVersion.capture(), serviceSchema.capture())
      schemaName.firstValue.should.startWith("vyne.casks.OrderWindowSummary-OrderWindowSummary")
      "1.0.0".should.equal(schemaVersion.firstValue)
      """namespace vyne.casks {

   type alias OrderWindowSummaryList as OrderWindowSummary[]

   @ServiceDiscoveryClient(serviceName = "cask")
   service OrderWindowSummaryCaskService {
      @HttpOperation(method = "GET" , url = "/api/cask/OrderWindowSummary/symbol/{Symbol}")
      operation findBySymbol( @PathVariable(name = "symbol") symbol : Symbol ) : OrderWindowSummaryList
      @HttpOperation(method = "GET" , url = "/api/cask/OrderWindowSummary/open/{Price}")
      operation findByOpen( @PathVariable(name = "open") open : Price ) : OrderWindowSummaryList
      @HttpOperation(method = "GET" , url = "/api/cask/OrderWindowSummary/high/{Price}")
      operation findByHigh( @PathVariable(name = "high") high : Price ) : OrderWindowSummaryList
      @HttpOperation(method = "GET" , url = "/api/cask/OrderWindowSummary/close/{Price}")
      operation findByClose( @PathVariable(name = "close") close : Price ) : OrderWindowSummaryList
   }
}

""".replace("\\s".toRegex(), "").should.equal(serviceSchema.firstValue.replace("\\s".toRegex(), ""))
   }
}
