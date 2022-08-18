package io.vyne.cask.ddl.views.taxiViews

import com.nhaarman.mockito_kotlin.eq
import com.nhaarman.mockito_kotlin.whenever
import io.vyne.VersionedSource
import io.vyne.asPackage
import io.vyne.cask.api.CaskConfig
import io.vyne.cask.config.CaskConfigRepository
import io.vyne.schema.api.SchemaSet
import io.vyne.schemaStore.SimpleSchemaStore
import io.vyne.schemas.VersionedType
import io.vyne.schemas.fqn
import io.vyne.schemas.taxi.TaxiSchema
import io.vyne.toParsedPackages
import lang.taxi.types.QualifiedName
import lang.taxi.types.View

object TestSchemas {
   fun fromSchemaSource(versionSource: VersionedSource, repository: CaskConfigRepository? = null): TestView {
      val taxiSchema = TaxiSchema.from(versionSource)
      val schemaStore = SimpleSchemaStore()
      schemaStore.setSchemaSet(SchemaSet.fromParsed(listOf(versionSource).asPackage().toParsedPackages(), 1))
      val taxiView = taxiSchema.document.views.first()
      val bodyTypes = taxiView.viewBodyDefinitions?.map { viewBodyDefinition -> viewBodyDefinition.bodyType }
         ?: emptyList()
      val joinTypes = taxiView.viewBodyDefinitions?.mapNotNull { viewBodyDefinition -> viewBodyDefinition.joinType }
         ?: emptyList()
      val typeNameToCaskConfigMap = bodyTypes.plus(joinTypes).toSet().map { type ->
        val pair = type.toQualifiedName() to (type.toQualifiedName() to CaskConfig.forType(
            VersionedType(emptyList(), taxiSchema.type(type.qualifiedName), type),
            "${type.toQualifiedName().typeName}_tb", daysToRetain = 100000
         ))
         repository?.let {
            whenever(it.findAllByQualifiedTypeName(eq(pair.first.fullyQualifiedName))).thenReturn(
               listOf(CaskConfig.forType(taxiSchema.versionedType(pair.first.fullyQualifiedName.fqn()), pair.second.second.tableName, daysToRetain = 100000)
               ))
         }
         pair
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
         type RequestedQuantity inherits Decimal
         type OrderStatus inherits String
         type DecimalFieldOrderFilled inherits Decimal
         type AggregatedCumulativeQty inherits Decimal
         type TradeNo inherits String
         type CumulativeQuantity inherits Decimal
         type RemainingQuantity inherits Decimal
         type DisplayedQuantity inherits Decimal
         type SellCumulativeQuantity inherits Decimal
         type BuyCumulativeQuantity inherits Decimal
         type DealerwebOrderBuy inherits String
         type DealerwebOrderSell inherits String
         type OrderBankDirection inherits String
         type TempCumulativeQuantity inherits Decimal
         type VenueStatus inherits String


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
            bankDirection: OrderBankDirection by default("BankBuys")
         }

         model OrderFill {
           @Id
           fillOrderId: FillOrderId
           orderType: OrderType by default("Market")
           subSecurityType: SecurityDescription? by column("Instrument Desc")
           executedQuantity: DecimalFieldOrderFilled? by column("Quantity")
           entryType: OrderStatus by default("Filled")
           tradeNo: TradeNo by column("TradeNo")
           orderBuy: DealerwebOrderBuy by column("bankbuys")
           orderSell: DealerwebOrderSell by column("ordersell")

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
              sellCumulativeQuantity: SellCumulativeQuantity
              buyCumulativeQuantity: BuyCumulativeQuantity
              cumulativeQty: CumulativeQuantity
              tempCumulativeQty: TempCumulativeQuantity
              venueStatus: VenueStatus

            },
            find { OrderSent[] (joinTo OrderFill[]) } as {
              orderId: OrderFill::FillOrderId
              orderDateTime: OrderEventDateTime
              orderType: OrderFill::OrderType
              subSecurityType: OrderFill::SecurityDescription
              requestedQuantity: OrderSent::RequestedQuantity
              orderEntry: OrderStatus by when {
                 OrderSent::RequestedQuantity == OrderView::CumulativeQuantity -> OrderFill::OrderStatus
                 else -> "PartiallyFilled"
              }
              leavesQuantity: RemainingQuantity by when {
                    OrderSent::RequestedQuantity == OrderFill::DecimalFieldOrderFilled -> 0
                    else -> (OrderSent::RequestedQuantity - OrderView::CumulativeQuantity)
              }
              displayQuantity: DisplayedQuantity by when {
                  OrderSent::RequestedQuantity == OrderFill::DecimalFieldOrderFilled -> 0
                  else -> OrderView::RemainingQuantity
              }
              tradeNo: OrderFill::TradeNo
              executedQuantity: OrderFill::DecimalFieldOrderFilled
              sellCumulativeQuantity: SellCumulativeQuantity by when{
                  OrderFill::TradeNo != null -> sumOver(OrderFill::DecimalFieldOrderFilled, OrderFill::DealerwebOrderSell, OrderFill::TradeNo)
                   else -> sumOver(OrderFill::DecimalFieldOrderFilled, OrderFill::DealerwebOrderSell)
               }
               buyCumulativeQuantity: BuyCumulativeQuantity by when{
                 OrderFill::TradeNo != null -> sumOver(OrderFill::DecimalFieldOrderFilled, OrderFill::DealerwebOrderBuy, OrderFill::TradeNo)
                 else -> sumOver(OrderFill::DecimalFieldOrderFilled, OrderFill::DealerwebOrderBuy)
               }
               cumulativeQty: CumulativeQuantity by when{
                  OrderSent::OrderBankDirection == "BankBuys" -> (OrderView::BuyCumulativeQuantity - OrderView::SellCumulativeQuantity)
                  else -> (OrderView::SellCumulativeQuantity - OrderView::BuyCumulativeQuantity)
                }
               tempCumulativeQty: TempCumulativeQuantity by sumOver(OrderFill::DecimalFieldOrderFilled, OrderFill::DealerwebOrderBuy)
               venueStatus: VenueStatus by when {
                    OrderFill::TradeNo != null && OrderSent::RequestedQuantity == OrderFill::DecimalFieldOrderFilled -> "venue1"
                    OrderSent::RequestedQuantity == OrderView::TempCumulativeQuantity -> "venue2"
                    else -> null
                }
            }
         }
   """.trimIndent())
   val viewWithConstraints = VersionedSource.sourceOnly("""
      type OrderId inherits String
      type TradeId inherits String
      type TradeId inherits String
      type OrderStatus inherits String
      type Taxonomy inherits String


      model Order {
         @Id
         orderId: OrderId
         orderStatus: OrderStatus
         taxonomy: Taxonomy
      }

      model Trade {
         @Id
         orderId: OrderId
         tradeId: TradeId
      }

      view OrderView with query {
         find { Order[] ( (OrderStatus == 'Filled' or OrderStatus == 'Partially Filled' and OrderStatus != 'Rejected') and ( Taxonomy in ['taxonomy1' , 'taxonomy2']) and (OrderId not in ['KFXXXX']) ) (joinTo Trade[]) } as {
             orderId: Order::OrderId
             tradeId: Trade::TradeId
         }
      }





   """.trimIndent())
   val viewWithMultipleConstraints = VersionedSource.sourceOnly("""
      type OrderId inherits String
      type TradeId inherits String
      type TradeId inherits String
      type OrderStatus inherits String
      type Taxonomy inherits String


      model Order {
         @Id
         orderId: OrderId
         orderStatus: OrderStatus
         taxonomy: Taxonomy
      }

      model Trade {
         @Id
         orderId: OrderId
         tradeId: TradeId
      }

      view OrderView with query {
         find { Order[] ( (OrderStatus == 'Filled') ) } as {
             orderId: Order::OrderId
             tradeId: TradeId
         },
         find { Order[] ( (OrderStatus == 'Filled' or OrderStatus == 'Partially Filled') and ( Taxonomy in ['taxonomy1' , 'taxonomy2']) ) (joinTo Trade[]) } as {
             orderId: OrderId by coalesce(Order::OrderId, Trade::OrderId)
             tradeId: Trade::TradeId
         }
      }
   """.trimIndent())
   val viewWithAndConditionsWithMultipleParts = VersionedSource.sourceOnly("""
      //OrderSent::OrderBankDirection = "BankSell" && OrderFilled::MarketTradeId != null && OrderFilled::ExecutedQuantity
      import vyne.aggregations.sumOver
      type OrderBankDirection inherits String
      type MarketId inherits String
      type ExecutedQuantity inherits Decimal
      type SyntheticFlag inherits Int
      type CumulativeQty inherits Decimal

      model Order {
         direction: OrderBankDirection
         marketId: MarketId
         executedQuantity: ExecutedQuantity
         sFlag: SyntheticFlag
      }

      view Sample  with query{
         find {Order []} as {
            cumQty: CumulativeQty by when {
              Order::OrderBankDirection == "Sell" && Order::MarketId != null && Order::ExecutedQuantity !=null && Order::SyntheticFlag == 1 -> sumOver(Order::ExecutedQuantity, Order::OrderBankDirection)
              else -> 0
            }

            sellQty: Decimal by when {
              Order::OrderBankDirection == "Sell" || Order::MarketId != null || Order::ExecutedQuantity !=null || Order::SyntheticFlag == 1 -> sumOver(Order::ExecutedQuantity, Order::OrderBankDirection)
              else -> 0
            }
        }
      }
   """.trimIndent())
   val viewWithAWhenStatementUsingEnumAssignments = VersionedSource.sourceOnly("""
      enum OrderStatus {
         Filled,
         PartiallyFilled,
         Active,
         Cancelled,
         Rejected
      }

      type OrderQty inherits Decimal
      type ExecutedQty inherits Decimal
      model Order {
         orderQty: OrderQty
         executedQty: ExecutedQty
         status: OrderStatus
      }

      view OrderView with query {
         find {Order[]} as {
            venueOrderStatus: OrderStatus by when {
               Order::OrderQty == Order::ExecutedQty  -> OrderStatus.Filled
               else -> OrderStatus.Active
            }
         }
      }
   """.trimIndent())

    val viewWithSubsctraction = VersionedSource.sourceOnly("""
      import vyne.aggregations.sumOver
      type CumulativeQty inherits Decimal
      type OrderId inherits String
      type ExecutedQuantity inherits Decimal
      type OrderEventDateTime inherits Instant
      type RequestedQuantity inherits Decimal
      type RemainingQuantity inherits Decimal


      model Order {
         orderId: OrderId
         execQty: ExecutedQuantity
         ts: OrderEventDateTime
         reqQty: RequestedQuantity
      }


      view Report with query {
         find {Order[]} as {
               sellCumulativeQuantity:  CumulativeQty by sumOver(Order:: ExecutedQuantity, Order:: OrderId, Order:: OrderEventDateTime)
               buyCumulativeQuantity: RequestedQuantity by sumOver(Order:: RequestedQuantity, Order:: OrderId, Order:: OrderEventDateTime)
               remainingQuantity: RemainingQuantity by (Report:: RequestedQuantity - Report:: CumulativeQty)
         }
      }
   """.trimIndent())
}

data class TestView(
   val taxiSchema: TaxiSchema,
   val schemaStore: SimpleSchemaStore,
   val taxiView: View,
   val tableNameMap: Map<QualifiedName, Pair<QualifiedName, CaskConfig>>)
