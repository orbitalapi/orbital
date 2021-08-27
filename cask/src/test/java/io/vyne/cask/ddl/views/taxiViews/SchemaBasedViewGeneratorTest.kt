package io.vyne.cask.ddl.views.taxiViews

import com.nhaarman.mockito_kotlin.eq
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.whenever
import com.winterbe.expekt.should
import io.vyne.VersionedSource
import io.vyne.cask.api.CaskConfig
import io.vyne.cask.config.CaskConfigRepository
import io.vyne.cask.ddl.views.taxiViews.TestSchemas.viewWithConstraints
import io.vyne.cask.ddl.views.taxiViews.TestSchemas.viewWithMultipleConstraints
import io.vyne.schemaStore.SchemaSet
import io.vyne.schemaStore.SimpleSchemaStore
import io.vyne.schemas.fqn
import io.vyne.schemas.taxi.TaxiSchema
import io.vyne.utils.withoutWhitespace
import net.sf.jsqlparser.util.validation.Validation
import net.sf.jsqlparser.util.validation.feature.DatabaseType
import org.junit.Assert
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
         listOf(CaskConfig.forType(taxiSchema.versionedType("OrderSent".fqn()), "ordersent", daysToRetain = 100000)
         ))

      val output =  SchemaBasedViewGenerator(repository, schemaStore).generateDdl(taxiSchema.document.views.first())
      output.size.should.equal(2)
      output[0].should.equal("""
         drop view if exists v_OrderView;
      """.trimIndent())
      Assert.assertEquals(output[1].toLowerCase().withoutWhitespace(),"""
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
      """.trimIndent().toLowerCase().withoutWhitespace())
   }

   @Test
   fun `generate sql view for a taxi view with two find statements, one with join another one without a join`() {
      val (taxiSchema, schemaStore) = fromSchemaSource(versionedSourceForViewWithTwoFindsOneWithAJoin)
      whenever(repository.findAllByQualifiedTypeName(eq("OrderSent"))).thenReturn(
         listOf(CaskConfig.forType(taxiSchema.versionedType("OrderSent".fqn()), "ordersent", daysToRetain = 100000)
         ))

      whenever(repository.findAllByQualifiedTypeName(eq("OrderFill"))).thenReturn(
         listOf(CaskConfig.forType(taxiSchema.versionedType("OrderFill".fqn()), "orderfill", daysToRetain = 100000)
         ))

      val output =  SchemaBasedViewGenerator(repository, schemaStore).generateDdl(taxiSchema.document.views.first())
      output.size.should.equal(2)
      Assert.assertEquals(
         """
         create or replace
            view v_OrderView as
            select
               ordersent."sentOrderId" as "orderId",
               ordersent."orderDateTime" as "orderDateTime",
               ordersent."orderType" as "orderType",
               ordersent."subSecurityType" as "subSecurityType",
               ordersent."requestedQuantity" as "requestedQuantity",
               ordersent."entryType" as "orderEntry",
               ordersent."remainingQuantity" as "leavesQuantity",
               ordersent."displayedQuantity" as "displayQuantity",
               null::VARCHAR(255) as "tradeNo",
               null::NUMERIC as "executedQuantity",
               null::NUMERIC as "cumulativeQty",
               "ordersent".caskmessageid as caskmessageid
            from
               ordersent
            union all
            select
               orderfill."fillOrderId" as "orderId",
               null::TIMESTAMP as "orderDateTime",
               orderfill."orderType" as "orderType",
               orderfill."subSecurityType" as "subSecurityType",
               ordersent."requestedQuantity" as "requestedQuantity",
               case
                  when ordersent."requestedQuantity" = orderfill."executedQuantity" then orderfill."entryType"
                  else 'PartiallyFilled'
               end as "orderEntry",
               case
                  when ordersent."requestedQuantity" = orderfill."executedQuantity" then 0
                  else
                  case
                     when orderfill."tradeNo" is null then ordersent."requestedQuantity" - orderfill."executedQuantity"
                     else ordersent."requestedQuantity" - SUM (orderfill."executedQuantity") OVER (PARTITION BY orderfill."fillOrderId" ORDER BY orderfill."tradeNo")
                  end
               end as "leavesQuantity",
               case
                  when ordersent."requestedQuantity" = orderfill."executedQuantity" then 0
                  else
                  case
                     when ordersent."requestedQuantity" = orderfill."executedQuantity" then 0
                     else
                     case
                        when orderfill."tradeNo" is null then ordersent."requestedQuantity" - orderfill."executedQuantity"
                        else ordersent."requestedQuantity" - SUM (orderfill."executedQuantity") OVER (PARTITION BY orderfill."fillOrderId" ORDER BY orderfill."tradeNo")
                     end
                  end
               end as "displayQuantity",
               orderfill."tradeNo" as "tradeNo",
               orderfill."executedQuantity" as "executedQuantity",
               case
                  when orderfill."tradeNo" is null then orderfill."executedQuantity"
                  else SUM (orderfill."executedQuantity") OVER (PARTITION BY orderfill."fillOrderId" ORDER BY orderfill."tradeNo")
               end as "cumulativeQty",
               (( SELECT get_later_messsageid("ordersent".caskmessageid, "orderfill".caskmessageid) AS get_later_messsageid))::charactervarying(40) AS caskmessageid
            from
               ordersent
            LEFT JOIN orderfill ON
               ordersent."sentOrderId" = orderfill."fillOrderId"
      """.trimIndent().withoutWhitespace().toLowerCase(),
         output[1].withoutWhitespace().toLowerCase())
      // validate the query that we've generate.
      Validation( listOf(DatabaseType.POSTGRESQL), output[1]).validate().should.be.empty
   }

   @Test
   fun `generate cask config for a taxi view with one find statement without a join`() {
      val (taxiSchema, schemaStore) = fromSchemaSource(versionedSourceForSimpleView)
      whenever(repository.findAllByQualifiedTypeName(eq("OrderSent"))).thenReturn(
         listOf(CaskConfig.forType(taxiSchema.versionedType("OrderSent".fqn()), "ordersent", daysToRetain = 100000)
         ))

       val caskConfig = SchemaBasedViewGenerator(repository, schemaStore).generateCaskConfig(taxiSchema.document.views.first())
      caskConfig.tableName.should.equal("v_OrderView")
      caskConfig.sources.first().withoutWhitespace().should.equal("""
         [[ Generated by Taxi View. ]]
         model OrderView inherits OrderEvent {
            orderId : SentOrderId?
            orderDateTime : OrderEventDateTime?
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
         listOf(CaskConfig.forType(taxiSchema.versionedType("OrderSent".fqn()), "ordersent", daysToRetain = 100000)
         ))

      whenever(repository.findAllByQualifiedTypeName(eq("OrderFill"))).thenReturn(
         listOf(CaskConfig.forType(taxiSchema.versionedType("OrderFill".fqn()), "orderfill", daysToRetain = 100000)
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
            leavesQuantity : RemainingQuantity?
            displayQuantity : DisplayedQuantity?
            tradeNo : TradeNo?
            executedQuantity : DecimalFieldOrderFilled?
            cumulativeQty : CumulativeQuantity?
         }


      """.withoutWhitespace()
         .trimIndent())
   }

   @Test
   fun `constraint based view definition`() {
      val testView = TestSchemas.fromSchemaSource(viewWithConstraints, repository)
      val output =  SchemaBasedViewGenerator(repository, testView.schemaStore).generateDdl(testView.taxiView)
      output.size.should.equal(2)
      Assert.assertEquals(output[1].withoutWhitespace().toLowerCase(), """
         create or replace view v_OrderView as
         select
         Order_tb."orderId" as "orderId",
         Trade_tb."tradeId" as "tradeId",
         (( SELECT get_later_messsageid("Order_tb".caskmessageid, "Trade_tb".caskmessageid) AS get_later_messsageid))::character varying(40) AS caskmessageid
          from Order_tb LEFT JOIN Trade_tb ON Order_tb."orderId" = Trade_tb."orderId"
          WHERE ((( ( Order_tb."orderStatus" = 'Filled'  OR  ( Order_tb."orderStatus" = 'Partially Filled'  AND   Order_tb."orderStatus" <> 'Rejected' )) ) AND  (  Order_tb."taxonomy" IN ( 'taxonomy1','taxonomy2' )  )) AND  (  Order_tb."orderId" NOT IN ( 'KFXXXX' )  ))
           """
         .trimIndent().toLowerCase().withoutWhitespace())
      // validate the query that we've generate.
      Validation( listOf(DatabaseType.POSTGRESQL), output[1]).validate().should.be.empty
   }

   @Test
   fun `constraint based view definition with two find statements`() {
      val testView = TestSchemas.fromSchemaSource(viewWithMultipleConstraints, repository)
      val output = SchemaBasedViewGenerator(repository, testView.schemaStore).generateDdl(testView.taxiView)
      output.size.should.equal(2)
      output[1].withoutWhitespace().should.equal("""
         create or replace view v_OrderView as
         select
            Order_tb."orderId" as "orderId",
            null::VARCHAR(255) as "tradeId",
            "Order_tb".caskmessageid as caskmessageid
             from Order_tb
             WHERE (  Order_tb."orderStatus" = 'Filled'  ) union all
            select
            COALESCE(Order_tb."orderId", Trade_tb."orderId") as "orderId",
            Trade_tb."tradeId" as "tradeId",
            (( SELECT get_later_messsageid("Order_tb".caskmessageid, "Trade_tb".caskmessageid) AS get_later_messsageid))::character varying(40) AS caskmessageid
             from Order_tb LEFT JOIN Trade_tb ON Order_tb."orderId" = Trade_tb."orderId"
          WHERE (( ( Order_tb."orderStatus" = 'Filled'  OR   Order_tb."orderStatus" = 'Partially Filled' ) ) AND  (  Order_tb."taxonomy" IN ( 'taxonomy1','taxonomy2' )  ))
 """.trimIndent().withoutWhitespace())
      // validate the query that we've generate.
      Validation(listOf(DatabaseType.POSTGRESQL), output[1]).validate().should.be.empty
   }

   fun `documentation sample`() {
      val (taxiSchema, schemaStore) = fromSchemaSource(VersionedSource.sourceOnly("""
         // sumOver is in vyne.aggregations namespace, so import it to use it.
         import vyne.aggregations.sumOver
         type OrderId inherits String
         type OrderQty inherits Decimal
         type TradeNo inherits Int
         model Order {
            orderId: OrderId
            qty: OrderQty
            tradeNo: TradeNo
         }

         view OrderView with query {
            find { Order[] } as {
               orderId: Order::OrderId
               // sumOver operates as a window function that operates on a set of items
               // specified by the second argument.
               // second argument divides contents of the 'view' into multiple partitions to which the 'sum'
               // function is applied. The third argument of the 'sumOver' specifies the order of the items in each partition the
               // 'sum' function is applied.
               // Therefore, cumulativeQty field is set to sum 'OrderQty' values on the partition defined by OrderId.
               // Partitions are ordered by Trade No.
               cumulativeQty: OrderQty by  sumOver(Order::OrderQty, Order::OrderId, Order::TradeNo)
            }
         }
      """.trimIndent()))

      whenever(repository.findAllByQualifiedTypeName(eq("Order"))).thenReturn(
         listOf(CaskConfig.forType(taxiSchema.versionedType("Order".fqn()), "order", daysToRetain = 100000)
         ))

      val output =  SchemaBasedViewGenerator(repository, schemaStore).generateDdl(taxiSchema.document.views.first())
      output.size.should.equal(2)
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
              orderId: OrderSent::SentOrderId
              orderDateTime: OrderSent::OrderEventDateTime
              orderType: OrderSent::OrderType
              subSecurityType: OrderSent::SecurityDescription
              requestedQuantity: OrderSent::RequestedQuantity
              orderEntry: OrderSent::OrderStatus
              orderSize: OrderSize by when {
                 OrderSent::RequestedQuantity = 0 -> "Zero Size"
                 OrderSent::RequestedQuantity > 0 && OrderSent::RequestedQuantity < 100 -> "Small Size"
                 else -> "PartiallyFilled"
              }
            }
         }
           """.trimIndent())
   private val versionedSourceForViewWithTwoFindsOneWithAJoin = VersionedSource.sourceOnly("""
         import vyne.aggregations.sumOver
         type SentOrderId inherits String
         type FillOrderId inherits String
         type OrderEventDateTime inherits Instant
         type OrderType inherits String
         type SecurityDescription inherits String
         type RequestedQuantity inherits String
         type OrderStatus inherits String
         type DecimalFieldOrderFilled inherits Decimal
         type AggregatedCumulativeQty inherits Decimal
         type TradeNo inherits String
         type CumulativeQuantity inherits Decimal
         type RemainingQuantity inherits Decimal
         type DisplayedQuantity inherits Decimal


         model OrderSent {
            @Id
            sentOrderId : SentOrderId
            @Between
		      orderDateTime: OrderEventDateTime( @format = "MM/dd/yy HH:mm:ss") by column("Time Submitted")
            orderType: OrderType by default("Market")
            subSecurityType: SecurityDescription? by column("Instrument Desc")
            requestedQuantity: RequestedQuantity? by column("Size")
            remainingQuantity: RemainingQuantity? by column("Size")
            displayedQuantity: DisplayedQuantity? by column("Size")
            entryType: OrderStatus by default("New")
         }

         model OrderFill {
           @Id
           fillOrderId: FillOrderId
           orderType: OrderType by default("Market")
           subSecurityType: SecurityDescription? by column("Instrument Desc")
           executedQuantity: DecimalFieldOrderFilled? by column("Quantity")
           entryType: OrderStatus by default("Filled")
           tradeNo: TradeNo by column("TradeNo")
         }

         model OrderEvent { }

          [[
           Sample View
          ]]
         view OrderView inherits OrderEvent with query {
            find { OrderSent[] } as {
              orderId: OrderSent::SentOrderId
              orderDateTime: OrderSent::OrderEventDateTime( @format = "MM/dd/yy HH:mm:ss")
              orderType: OrderSent::OrderType
              subSecurityType: OrderSent::SecurityDescription
              requestedQuantity: OrderSent::RequestedQuantity
              orderEntry: OrderSent::OrderStatus
              leavesQuantity: OrderSent::RemainingQuantity
              displayQuantity: OrderSent::DisplayedQuantity
              tradeNo: TradeNo
              executedQuantity: DecimalFieldOrderFilled
              cumulativeQty: CumulativeQuantity
            },
            find { OrderSent[] (joinTo OrderFill[]) } as {
              orderId: OrderFill::FillOrderId
              orderDateTime: OrderEventDateTime
              orderType: OrderFill::OrderType
              subSecurityType: OrderFill::SecurityDescription
              requestedQuantity: OrderSent::RequestedQuantity
              orderEntry: OrderStatus by when {
                 OrderSent::RequestedQuantity = OrderFill::DecimalFieldOrderFilled -> OrderFill::OrderStatus
                 else -> "PartiallyFilled"
              }
              leavesQuantity: RemainingQuantity by when {
                    OrderSent::RequestedQuantity = OrderFill::DecimalFieldOrderFilled -> 0
                    else -> (OrderSent::RequestedQuantity - OrderView::CumulativeQuantity)
              }
              displayQuantity: DisplayedQuantity by when {
                  OrderSent::RequestedQuantity = OrderFill::DecimalFieldOrderFilled -> 0
                  else -> OrderView::RemainingQuantity
              }
              tradeNo: OrderFill::TradeNo
              executedQuantity: OrderFill::DecimalFieldOrderFilled
              cumulativeQty: CumulativeQuantity by when {
                  OrderFill::TradeNo = null -> OrderFill::DecimalFieldOrderFilled
                  else -> sumOver(OrderFill::DecimalFieldOrderFilled, OrderFill::FillOrderId, OrderFill::TradeNo)
                }
            }
         }
   """.trimIndent())
}
