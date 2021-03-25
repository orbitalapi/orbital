package io.vyne.cask.ddl.views

import com.nhaarman.mockito_kotlin.eq
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.whenever
import com.winterbe.expekt.should
import io.vyne.VersionedSource
import io.vyne.cask.api.CaskConfig
import io.vyne.cask.config.CaskConfigRepository
import io.vyne.schemaStore.SchemaSet
import io.vyne.schemaStore.SimpleSchemaStore
import io.vyne.schemas.fqn
import io.vyne.schemas.taxi.TaxiSchema
import org.junit.Test

class SchemaBasedViewGeneratorTest {
   lateinit var repository: CaskConfigRepository

   @Test
   fun `generate sql view for a taxi view with one find statement`() {
      val src = VersionedSource.sourceOnly("""
         type SentOrderId inherits String
         type FillOrderId inherits String
         type OrderEventDateTime inherits Instant
         type OrderType inherits String
         type SecurityDescription inherits String
         type RequestedQuantity inherits String
         type OrderStatus inherits String
         type DecimalFieldOrderFilled inherits Decimal
         type OrderSize inherits String

         model OrderSent {
            @Id
            sentOrderId : SentOrderId
            @Between
		      orderDateTime: OrderEventDateTime( @format = "MM/dd/yy HH:mm:ss") by column("Time Submitted")
            orderType: OrderType by default("Market")
            subSecurityType: SecurityDescription? by column("Instrument Desc")
            requestedQuantity: RequestedQuantity? by column("Size")
            entryType: OrderStatus by default("New")
         }

         model OrderEvent { }



          [[
           Sample View
          ]]
         view OrderView inherits OrderEvent with query {
            find { OrderSent[] } as {
              orderId: OrderSent.SentOrderId
              orderDateTime: OrderSent.OrderEventDateTime
              orderType: OrderSent.OrderType
              subSecurityType: OrderSent.SecurityDescription
              requestedQuantity: OrderSent.RequestedQuantity
              orderEntry: OrderSent.OrderStatus
              orderSize: OrderSize by when {
                 OrderSent.RequestedQuantity = 0 -> "Zero Size"
                 OrderSent.RequestedQuantity > 0 && OrderSent.RequestedQuantity < 100 -> "Small Size"
                 else -> "PartiallyFilled"
              }
            }
         }
           """.trimIndent())
      val taxiSchema = TaxiSchema.from(src)
      val schemaStore = SimpleSchemaStore()
      repository = mock { }
      whenever(repository.findAllByQualifiedTypeName(eq("OrderSent"))).thenReturn(
         listOf(CaskConfig.forType(taxiSchema.versionedType("OrderSent".fqn()), "ordersent")
         ))
      schemaStore.setSchemaSet(SchemaSet.from(listOf(src), 1))
      val output =  SchemaBasedViewGenerator(repository, schemaStore).generateDdl(taxiSchema.document.views.first())
      output.size.should.not.equal(0)
   }
}
