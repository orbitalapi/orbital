package io.vyne.cask.ddl.views.taxiViews

import com.winterbe.expekt.should
import io.vyne.utils.withoutWhitespace
import lang.taxi.types.ObjectType
import net.sf.jsqlparser.parser.CCJSqlParserUtil
import org.junit.Assert
import org.junit.Test

class WhenStatementGeneratorTest {
   private val testView  =TestSchemas.fromSchemaSource(TestSchemas.versionedSourceForViewWithTwoFindsOneWithAJoin)
   private val viewDefinitionWithJoinPart = testView.taxiView.viewBodyDefinitions!![1]
   private val objectType = (viewDefinitionWithJoinPart.viewBodyType!! as ObjectType)
   private val typeUnderTest = WhenStatementGenerator(testView.taxiView, testView.taxiView.viewBodyDefinitions!![1].viewBodyType!! as ObjectType, testView.tableNameMap, testView.taxiSchema)
   /*
   *
   *            cumulativeQty: CumulativeQuantity by when {
   *               OrderFill::TradeNo = null -> OrderFill::DecimalFieldOrderFilled
   *               else -> sumOver(OrderFill::DecimalFieldOrderFilled, OrderFill::FillOrderId, OrderFill::TradeNo)
   *             }
   *
   * */
   @Test
   fun `should generate case sql for a field with an aggregation function on the assignment part of a when statement`() {
      val caseStatement = """
         CASE
            WHEN ordersent_e9a9fa_cafb68."requestedQuantity" = orderfilled_2128e6_a98274."executedQuantity" THEN 0::numeric
            ELSE
            CASE
                WHEN orderfilled_2128e6_a98274."marketTradeId"::text = NULL::text THEN ordersent_e9a9fa_cafb68."requestedQuantity" - orderfilled_2128e6_a98274."executedQuantity"
                ELSE ordersent_e9a9fa_cafb68."requestedQuantity" - sum(orderfilled_2128e6_a98274."executedQuantity") OVER (PARTITION BY orderfilled_2128e6_a98274."dealerwebFilledOrderId" ORDER BY orderfilled_2128e6_a98274."marketTradeId")
            END
        END AS "leavesQuantity"
      """.trimIndent()
      val sql = typeUnderTest.toWhenSql(objectType.field("cumulativeQty"))
      val simpleExpress = CCJSqlParserUtil.parseExpression("""
         null::TIMESTAMP as "orderDateTime"
      """.trimIndent(), true)
      val result =  CCJSqlParserUtil.parseCondExpression(sql.toString(), true)
      sql.toString().toLowerCase().trimIndent().withoutWhitespace().should.equal(
         """
            case
	when OrderSent_tb."bankDirection" = 'BankBuys' then
	case
		when OrderFill_tb."tradeNo" <> null
		and OrderFill_tb."tradeNo" <> null then SUM(OrderFill_tb."executedQuantity") over (partition by OrderFill_tb."orderBuy"
	order by
		OrderFill_tb."tradeNo") - SUM(OrderFill_tb."executedQuantity") over (partition by OrderFill_tb."orderSell"
	order by
		OrderFill_tb."tradeNo")
		when OrderFill_tb."tradeNo" <> null then SUM(OrderFill_tb."executedQuantity") over (partition by OrderFill_tb."orderSell"
	order by
		OrderFill_tb."tradeNo") - SUM(OrderFill_tb."executedQuantity") over (partition by OrderFill_tb."orderBuy" )
		when OrderFill_tb."tradeNo" <> null then SUM(OrderFill_tb."executedQuantity") over (partition by OrderFill_tb."orderBuy"
	order by
		OrderFill_tb."tradeNo") - SUM(OrderFill_tb."executedQuantity") over (partition by OrderFill_tb."orderSell" )
		else SUM(OrderFill_tb."executedQuantity") over (partition by OrderFill_tb."orderBuy" ) - SUM(OrderFill_tb."executedQuantity") over (partition by OrderFill_tb."orderSell" )
	end
	else
	case
		when OrderFill_tb."tradeNo" <> null
		and OrderFill_tb."tradeNo" <> null then SUM(OrderFill_tb."executedQuantity") over (partition by OrderFill_tb."orderSell"
	order by
		OrderFill_tb."tradeNo") - SUM(OrderFill_tb."executedQuantity") over (partition by OrderFill_tb."orderBuy"
	order by
		OrderFill_tb."tradeNo")
		when OrderFill_tb."tradeNo" <> null then SUM(OrderFill_tb."executedQuantity") over (partition by OrderFill_tb."orderBuy"
	order by
		OrderFill_tb."tradeNo") - SUM(OrderFill_tb."executedQuantity") over (partition by OrderFill_tb."orderSell" )
		when OrderFill_tb."tradeNo" <> null then SUM(OrderFill_tb."executedQuantity") over (partition by OrderFill_tb."orderSell"
	order by
		OrderFill_tb."tradeNo") - SUM(OrderFill_tb."executedQuantity") over (partition by OrderFill_tb."orderBuy" )
		else SUM(OrderFill_tb."executedQuantity") over (partition by OrderFill_tb."orderSell" ) - SUM(OrderFill_tb."executedQuantity") over (partition by OrderFill_tb."orderBuy" )
	end
end
         """.toLowerCase().trimIndent().withoutWhitespace()
      )
   }

   /*
   *  Note that this field has a reference for
   *   leavesQuantity: RemainingQuantity by when {
   *             OrderSent::RequestedQuantity = OrderFill::DecimalFieldOrderFilled -> 0
   *             else -> (OrderSent::RequestedQuantity - OrderView::CumulativeQuantity)
   *            }
   *
    */
   @Test
   fun `should generate case sql for a field which references another view field in the assignment part of a when statement`() {
      val sql = typeUnderTest.toWhenSql(objectType.field("leavesQuantity"))
      Assert.assertEquals(sql.toString().toLowerCase().trimIndent().withoutWhitespace(),
         """
            case
	when OrderSent_tb."requestedQuantity" = OrderFill_tb."executedQuantity" then 0
	else
	case
		when OrderSent_tb."bankDirection" = 'BankBuys' then OrderSent_tb."requestedQuantity" -
		case
			when OrderFill_tb."tradeNo" <> null
			and OrderFill_tb."tradeNo" <> null then SUM(OrderFill_tb."executedQuantity") over (partition by OrderFill_tb."orderBuy"
		order by
			OrderFill_tb."tradeNo") - SUM(OrderFill_tb."executedQuantity") over (partition by OrderFill_tb."orderSell"
		order by
			OrderFill_tb."tradeNo")
			when OrderFill_tb."tradeNo" <> null then SUM(OrderFill_tb."executedQuantity") over (partition by OrderFill_tb."orderSell"
		order by
			OrderFill_tb."tradeNo") - SUM(OrderFill_tb."executedQuantity") over (partition by OrderFill_tb."orderBuy" )
			when OrderFill_tb."tradeNo" <> null then SUM(OrderFill_tb."executedQuantity") over (partition by OrderFill_tb."orderBuy"
		order by
			OrderFill_tb."tradeNo") - SUM(OrderFill_tb."executedQuantity") over (partition by OrderFill_tb."orderSell" )
			else SUM(OrderFill_tb."executedQuantity") over (partition by OrderFill_tb."orderBuy" ) - SUM(OrderFill_tb."executedQuantity") over (partition by OrderFill_tb."orderSell" )
		end
		else OrderSent_tb."requestedQuantity" -
		case
			when OrderFill_tb."tradeNo" <> null
			and OrderFill_tb."tradeNo" <> null then SUM(OrderFill_tb."executedQuantity") over (partition by OrderFill_tb."orderSell"
		order by
			OrderFill_tb."tradeNo") - SUM(OrderFill_tb."executedQuantity") over (partition by OrderFill_tb."orderBuy"
		order by
			OrderFill_tb."tradeNo")
			when OrderFill_tb."tradeNo" <> null then SUM(OrderFill_tb."executedQuantity") over (partition by OrderFill_tb."orderBuy"
		order by
			OrderFill_tb."tradeNo") - SUM(OrderFill_tb."executedQuantity") over (partition by OrderFill_tb."orderSell" )
			when OrderFill_tb."tradeNo" <> null then SUM(OrderFill_tb."executedQuantity") over (partition by OrderFill_tb."orderSell"
		order by
			OrderFill_tb."tradeNo") - SUM(OrderFill_tb."executedQuantity") over (partition by OrderFill_tb."orderBuy" )
			else SUM(OrderFill_tb."executedQuantity") over (partition by OrderFill_tb."orderSell" ) - SUM(OrderFill_tb."executedQuantity") over (partition by OrderFill_tb."orderBuy" )
		end
	end
end
         """.toLowerCase().trimIndent().withoutWhitespace()
            )
   }

   /**
    *   Note that this field has a reference for  OrderView::RemainingQuantity which points to leavesQuantity which in turn makes use of
    *   OrderView::CumulativeQuantity
    *  displayQuantity: DisplayedQuantity by when {
    *               OrderSent::RequestedQuantity = OrderFill::DecimalFieldOrderFilled -> 0
    *               else -> OrderView::RemainingQuantity
    *           }
    */
   @Test
   fun `should generate case sql for a field which references another view field which also references another view field in the assignment part of a when statement`() {
      val sql = typeUnderTest.toWhenSql(objectType.field("displayQuantity"))
      sql.toString().toLowerCase().trimIndent().withoutWhitespace().replace(" ", "").should.equal(
         """
            case
	when OrderSent_tb."requestedQuantity" = OrderFill_tb."executedQuantity" then 0
	else
	case
		when OrderSent_tb."requestedQuantity" = OrderFill_tb."executedQuantity" then 0
		else
		case
			when OrderSent_tb."bankDirection" = 'BankBuys' then OrderSent_tb."requestedQuantity" -
			case
				when OrderFill_tb."tradeNo" <> null
				and OrderFill_tb."tradeNo" <> null then SUM(OrderFill_tb."executedQuantity") over (partition by OrderFill_tb."orderBuy"
			order by
				OrderFill_tb."tradeNo") - SUM(OrderFill_tb."executedQuantity") over (partition by OrderFill_tb."orderSell"
			order by
				OrderFill_tb."tradeNo")
				when OrderFill_tb."tradeNo" <> null then SUM(OrderFill_tb."executedQuantity") over (partition by OrderFill_tb."orderSell"
			order by
				OrderFill_tb."tradeNo") - SUM(OrderFill_tb."executedQuantity") over (partition by OrderFill_tb."orderBuy" )
				when OrderFill_tb."tradeNo" <> null then SUM(OrderFill_tb."executedQuantity") over (partition by OrderFill_tb."orderBuy"
			order by
				OrderFill_tb."tradeNo") - SUM(OrderFill_tb."executedQuantity") over (partition by OrderFill_tb."orderSell" )
				else SUM(OrderFill_tb."executedQuantity") over (partition by OrderFill_tb."orderBuy" ) - SUM(OrderFill_tb."executedQuantity") over (partition by OrderFill_tb."orderSell" )
			end
			else OrderSent_tb."requestedQuantity" -
			case
				when OrderFill_tb."tradeNo" <> null
				and OrderFill_tb."tradeNo" <> null then SUM(OrderFill_tb."executedQuantity") over (partition by OrderFill_tb."orderSell"
			order by
				OrderFill_tb."tradeNo") - SUM(OrderFill_tb."executedQuantity") over (partition by OrderFill_tb."orderBuy"
			order by
				OrderFill_tb."tradeNo")
				when OrderFill_tb."tradeNo" <> null then SUM(OrderFill_tb."executedQuantity") over (partition by OrderFill_tb."orderBuy"
			order by
				OrderFill_tb."tradeNo") - SUM(OrderFill_tb."executedQuantity") over (partition by OrderFill_tb."orderSell" )
				when OrderFill_tb."tradeNo" <> null then SUM(OrderFill_tb."executedQuantity") over (partition by OrderFill_tb."orderSell"
			order by
				OrderFill_tb."tradeNo") - SUM(OrderFill_tb."executedQuantity") over (partition by OrderFill_tb."orderBuy" )
				else SUM(OrderFill_tb."executedQuantity") over (partition by OrderFill_tb."orderSell" ) - SUM(OrderFill_tb."executedQuantity") over (partition by OrderFill_tb."orderBuy" )
			end
		end
	end
end
         """.toLowerCase().trimIndent().withoutWhitespace()
      )
   }

   @Test
   fun `should generate Sql for when statement referencing a view field on the left hand side of a when clause`() {
      //venueStatus
      val sql = typeUnderTest.toWhenSql(objectType.field("venueStatus"))
      sql.toString().toLowerCase().trimIndent().withoutWhitespace()
   }

}
