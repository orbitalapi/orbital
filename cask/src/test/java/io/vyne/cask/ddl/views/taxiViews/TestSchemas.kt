package io.vyne.cask.ddl.views.taxiViews

import io.vyne.VersionedSource
import io.vyne.cask.api.CaskConfig
import io.vyne.schemaStore.SchemaSet
import io.vyne.schemaStore.SimpleSchemaStore
import io.vyne.schemas.VersionedType
import io.vyne.schemas.taxi.TaxiSchema
import lang.taxi.types.QualifiedName
import lang.taxi.types.View

object TestSchemas {
   fun fromSchemaSource(versionSource: VersionedSource): TestView {
      val taxiSchema = TaxiSchema.from(versionSource)
      val schemaStore = SimpleSchemaStore()
      schemaStore.setSchemaSet(SchemaSet.from(listOf(versionSource), 1))
      val taxiView = taxiSchema.document.views.first()
      val bodyTypes = taxiView.viewBodyDefinitions?.map { viewBodyDefinition -> viewBodyDefinition.bodyType }
         ?: emptyList()
      val joinTypes = taxiView.viewBodyDefinitions?.mapNotNull { viewBodyDefinition -> viewBodyDefinition.joinType }
         ?: emptyList()
      val typeNameToCaskConfigMap = bodyTypes.plus(joinTypes).toSet().map { type ->
         type.toQualifiedName() to (type.toQualifiedName() to CaskConfig.forType(
            VersionedType(emptyList(), taxiSchema.type(type.qualifiedName), type),
            "${type.toQualifiedName().typeName}_tb"
         ))
      }.toMap()

      return TestView(taxiSchema, schemaStore, taxiView, typeNameToCaskConfigMap)
   }

   val versionedSourceForViewWithTwoFindsOneWithAJoin = VersionedSource.sourceOnly("""
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

data class TestView(
   val taxiSchema: TaxiSchema,
   val schemaStore: SimpleSchemaStore,
   val taxiView: View,
   val tableNameMap: Map<QualifiedName, Pair<QualifiedName, CaskConfig>>)
