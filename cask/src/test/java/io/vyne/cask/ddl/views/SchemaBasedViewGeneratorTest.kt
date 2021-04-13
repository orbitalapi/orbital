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
import io.vyne.utils.withoutWhitespace
import org.junit.Test

class SchemaBasedViewGeneratorTest {
    val repository: CaskConfigRepository = mock { }

   private fun fromSchemaSource(versionSource: VersionedSource): Pair<TaxiSchema, SimpleSchemaStore> {
      val taxiSchema = TaxiSchema.from(versionSource)
      val schemaStore = SimpleSchemaStore()
      schemaStore.setSchemaSet(SchemaSet.from(listOf(versionSource), 1))
      return Pair(taxiSchema, schemaStore)
   }

   @Test
   fun `generate sql view for a taxi view with one find statement without a join`() {
      val (taxiSchema, schemaStore) = fromSchemaSource(versionedSourceForSimpleView)
      whenever(repository.findAllByQualifiedTypeName(eq("OrderSent"))).thenReturn(
         listOf(CaskConfig.forType(taxiSchema.versionedType("OrderSent".fqn()), "ordersent")
         ))

      val output =  SchemaBasedViewGenerator(repository, schemaStore).generateDdl(taxiSchema.document.views.first())
      output.size.should.equal(2)
      output[0].should.equal("""
         drop view if exists v_OrderView;
      """.trimIndent())
      output[1].withoutWhitespace().should.equal("""
         create or replace view v_OrderView as
            select
            ordersent."sentOrderId" as "orderId",
            ordersent."orderDateTime" as "orderDateTime",
            ordersent."orderType" as "orderType",
            ordersent."subSecurityType" as "subSecurityType",
            ordersent."requestedQuantity" as "requestedQuantity",
            ordersent."entryType" as "orderEntry",
            case
            when ordersent."requestedQuantity" = 0 then 'Zero Size'
            when ordersent."requestedQuantity" > 0 AND ordersent."requestedQuantity" < 100 then 'Small Size'
            else 'PartiallyFilled'
            end as "orderSize",
         "ordersent".caskmessageid as caskmessageid
         from ordersent
      """.trimIndent().withoutWhitespace())
   }

   @Test
   fun `generate sql view for a taxi view with two find statements, one with join another one without a join`() {
      val (taxiSchema, schemaStore) = fromSchemaSource(versionedSourceForViewWithTwoFindsOneWithAJoin)
      whenever(repository.findAllByQualifiedTypeName(eq("OrderSent"))).thenReturn(
         listOf(CaskConfig.forType(taxiSchema.versionedType("OrderSent".fqn()), "ordersent")
         ))

      whenever(repository.findAllByQualifiedTypeName(eq("OrderFill"))).thenReturn(
         listOf(CaskConfig.forType(taxiSchema.versionedType("OrderFill".fqn()), "orderfill")
         ))

      val output =  SchemaBasedViewGenerator(repository, schemaStore).generateDdl(taxiSchema.document.views.first())
      output.size.should.equal(2)
      output[1].withoutWhitespace().should.equal("""
         create or replace view v_OrderView as
            select
               ordersent."sentOrderId" as "orderId",
               ordersent."orderDateTime" as "orderDateTime",
               ordersent."orderType" as "orderType",
               ordersent."subSecurityType" as "subSecurityType",
               ordersent."requestedQuantity" as "requestedQuantity",
               ordersent."entryType" as "orderEntry",
               "ordersent".caskmessageid as caskmessageid
            from ordersent
             union all
            select distinct
                  orderfill."fillOrderId" as "orderId",
                  null::TIMESTAMP as "orderDateTime",
                  orderfill."orderType" as "orderType",
                  orderfill."subSecurityType" as "subSecurityType",
                  ordersent."requestedQuantity" as "requestedQuantity",
                  case
                     when ordersent."requestedQuantity" = orderfill."executedQuantity" then orderfill."entryType"
                     else 'PartiallyFilled'
                  end as "orderEntry",

            "ordersent".caskmessageid as caskmessageid
            from ordersent LEFT JOIN orderfill ON ordersent."sentOrderId" = orderfill."fillOrderId"
      """.trimIndent().withoutWhitespace())
   }

   @Test
   fun `generate cask config for a taxi view with one find statement without a join`() {
      val (taxiSchema, schemaStore) = fromSchemaSource(versionedSourceForSimpleView)
      whenever(repository.findAllByQualifiedTypeName(eq("OrderSent"))).thenReturn(
         listOf(CaskConfig.forType(taxiSchema.versionedType("OrderSent".fqn()), "ordersent")
         ))

       val caskConfig = SchemaBasedViewGenerator(repository, schemaStore).generateCaskConfig(taxiSchema.document.views.first())
      caskConfig.tableName.should.equal("v_OrderView")
      caskConfig.sources.first().withoutWhitespace().should.equal("""
         [[ Generated by Taxi View. ]]
         model OrderView inherits OrderEvent {
            orderId : SentOrderId?
            orderDateTime : OrderEventDateTime?( @format = "MM/dd/yy HH:mm:ss" )
            orderType : OrderType?
            subSecurityType : SecurityDescription?
            requestedQuantity : RequestedQuantity?
            orderEntry : OrderStatus?
            orderSize : OrderSize?
         }
      """.trimIndent().withoutWhitespace())


   }

   @Test
   fun `generate cask config a taxi view with two find statements, one with join another one without a join`() {
      val (taxiSchema, schemaStore) = fromSchemaSource(versionedSourceForViewWithTwoFindsOneWithAJoin)
      whenever(repository.findAllByQualifiedTypeName(eq("OrderSent"))).thenReturn(
         listOf(CaskConfig.forType(taxiSchema.versionedType("OrderSent".fqn()), "ordersent")
         ))

      whenever(repository.findAllByQualifiedTypeName(eq("OrderFill"))).thenReturn(
         listOf(CaskConfig.forType(taxiSchema.versionedType("OrderFill".fqn()), "orderfill")
         ))

      val caskConfig =  SchemaBasedViewGenerator(repository, schemaStore).generateCaskConfig(taxiSchema.document.views.first())
      caskConfig.tableName.should.equal("v_OrderView")
      caskConfig.sources.first().withoutWhitespace().should.equal("""
         [[ Generated by Taxi View. ]]
         model OrderView inherits OrderEvent {
            orderId : SentOrderId?
            orderDateTime : OrderEventDateTime?( @format = "MM/dd/yy HH:mm:ss" )
            orderType : OrderType?
            subSecurityType : SecurityDescription?
            requestedQuantity : RequestedQuantity?
            orderEntry : OrderStatus?
         }


      """.withoutWhitespace()
         .trimIndent())
   }

   private val versionedSourceForSimpleView = VersionedSource.sourceOnly("""
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
   private val versionedSourceForViewWithTwoFindsOneWithAJoin = VersionedSource.sourceOnly("""
      type SentOrderId inherits String
         type FillOrderId inherits String
         type OrderEventDateTime inherits Instant
         type OrderType inherits String
         type SecurityDescription inherits String
         type RequestedQuantity inherits String
         type OrderStatus inherits String
         type DecimalFieldOrderFilled inherits Decimal


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

         model OrderFill {
           @Id
           fillOrderId: FillOrderId
           orderType: OrderType by default("Market")
           subSecurityType: SecurityDescription? by column("Instrument Desc")
           executedQuantity: DecimalFieldOrderFilled? by column("Quantity")
           entryType: OrderStatus by default("Filled")
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
            },
            find { OrderSent[] (joinTo OrderFill[]) } as {
              orderId: OrderFill.FillOrderId
              orderDateTime: OrderEventDateTime
              orderType: OrderFill.OrderType
              subSecurityType: OrderFill.SecurityDescription
              requestedQuantity: OrderSent.RequestedQuantity
              orderEntry: OrderStatus by when {
                 OrderSent.RequestedQuantity = OrderFill.DecimalFieldOrderFilled -> OrderFill.OrderStatus
                 else -> "PartiallyFilled"
              }
            }
         }
   """.trimIndent())
}
