package io.vyne.cask.ddl.views.taxiViews

import com.winterbe.expekt.should
import io.vyne.utils.withoutWhitespace
import lang.taxi.types.ObjectType
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
      val sql = typeUnderTest.toWhenSql(objectType.field("cumulativeQty"))
      Assert.assertEquals(sql.toString().toLowerCase().trimIndent().withoutWhitespace(),
         """
            CASE
	WHEN OrderSent_tb."bankDirection" = 'BankBuys' THEN
	CASE
		WHEN OrderFill_tb."tradeNo" IS NOT NULL
		AND OrderFill_tb."tradeNo" IS NOT NULL THEN SUM(OrderFill_tb."executedQuantity") OVER (PARTITION BY OrderFill_tb."orderBuy"
	ORDER BY
		OrderFill_tb."tradeNo") - (SUM(OrderFill_tb."executedQuantity") OVER (PARTITION BY OrderFill_tb."orderSell"
	ORDER BY
		OrderFill_tb."tradeNo"))
		WHEN OrderFill_tb."tradeNo" IS NOT NULL THEN SUM(OrderFill_tb."executedQuantity") OVER (PARTITION BY OrderFill_tb."orderBuy" ) - (SUM(OrderFill_tb."executedQuantity") OVER (PARTITION BY OrderFill_tb."orderSell"
	ORDER BY
		OrderFill_tb."tradeNo"))
		WHEN OrderFill_tb."tradeNo" IS NOT NULL THEN SUM(OrderFill_tb."executedQuantity") OVER (PARTITION BY OrderFill_tb."orderBuy"
	ORDER BY
		OrderFill_tb."tradeNo") - (SUM(OrderFill_tb."executedQuantity") OVER (PARTITION BY OrderFill_tb."orderSell" ))
		ELSE SUM(OrderFill_tb."executedQuantity") OVER (PARTITION BY OrderFill_tb."orderBuy" ) - (SUM(OrderFill_tb."executedQuantity") OVER (PARTITION BY OrderFill_tb."orderSell" ))
	END
	ELSE
	CASE
		WHEN OrderFill_tb."tradeNo" IS NOT NULL
		AND OrderFill_tb."tradeNo" IS NOT NULL THEN SUM(OrderFill_tb."executedQuantity") OVER (PARTITION BY OrderFill_tb."orderSell"
	ORDER BY
		OrderFill_tb."tradeNo") - (SUM(OrderFill_tb."executedQuantity") OVER (PARTITION BY OrderFill_tb."orderBuy"
	ORDER BY
		OrderFill_tb."tradeNo"))
		WHEN OrderFill_tb."tradeNo" IS NOT NULL THEN SUM(OrderFill_tb."executedQuantity") OVER (PARTITION BY OrderFill_tb."orderSell" ) - (SUM(OrderFill_tb."executedQuantity") OVER (PARTITION BY OrderFill_tb."orderBuy"
	ORDER BY
		OrderFill_tb."tradeNo"))
		WHEN OrderFill_tb."tradeNo" IS NOT NULL THEN SUM(OrderFill_tb."executedQuantity") OVER (PARTITION BY OrderFill_tb."orderSell"
	ORDER BY
		OrderFill_tb."tradeNo") - (SUM(OrderFill_tb."executedQuantity") OVER (PARTITION BY OrderFill_tb."orderBuy" ))
		ELSE SUM(OrderFill_tb."executedQuantity") OVER (PARTITION BY OrderFill_tb."orderSell" ) - (SUM(OrderFill_tb."executedQuantity") OVER (PARTITION BY OrderFill_tb."orderBuy" ))
	END
END
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
            CASE
	WHEN OrderSent_tb."requestedQuantity" = OrderFill_tb."executedQuantity" THEN 0
	ELSE
	CASE
		WHEN OrderSent_tb."bankDirection" = 'BankBuys' THEN OrderSent_tb."requestedQuantity" -
		CASE
			WHEN OrderFill_tb."tradeNo" IS NOT NULL
			AND OrderFill_tb."tradeNo" IS NOT NULL THEN SUM(OrderFill_tb."executedQuantity") OVER (PARTITION BY OrderFill_tb."orderBuy"
		ORDER BY
			OrderFill_tb."tradeNo") - (SUM(OrderFill_tb."executedQuantity") OVER (PARTITION BY OrderFill_tb."orderSell"
		ORDER BY
			OrderFill_tb."tradeNo"))
			WHEN OrderFill_tb."tradeNo" IS NOT NULL THEN SUM(OrderFill_tb."executedQuantity") OVER (PARTITION BY OrderFill_tb."orderBuy" ) - (SUM(OrderFill_tb."executedQuantity") OVER (PARTITION BY OrderFill_tb."orderSell"
		ORDER BY
			OrderFill_tb."tradeNo"))
			WHEN OrderFill_tb."tradeNo" IS NOT NULL THEN SUM(OrderFill_tb."executedQuantity") OVER (PARTITION BY OrderFill_tb."orderBuy"
		ORDER BY
			OrderFill_tb."tradeNo") - (SUM(OrderFill_tb."executedQuantity") OVER (PARTITION BY OrderFill_tb."orderSell" ))
			ELSE SUM(OrderFill_tb."executedQuantity") OVER (PARTITION BY OrderFill_tb."orderBuy" ) - (SUM(OrderFill_tb."executedQuantity") OVER (PARTITION BY OrderFill_tb."orderSell" ))
		END
		ELSE OrderSent_tb."requestedQuantity" -
		CASE
			WHEN OrderFill_tb."tradeNo" IS NOT NULL
			AND OrderFill_tb."tradeNo" IS NOT NULL THEN SUM(OrderFill_tb."executedQuantity") OVER (PARTITION BY OrderFill_tb."orderSell"
		ORDER BY
			OrderFill_tb."tradeNo") - (SUM(OrderFill_tb."executedQuantity") OVER (PARTITION BY OrderFill_tb."orderBuy"
		ORDER BY
			OrderFill_tb."tradeNo"))
			WHEN OrderFill_tb."tradeNo" IS NOT NULL THEN SUM(OrderFill_tb."executedQuantity") OVER (PARTITION BY OrderFill_tb."orderSell" ) - (SUM(OrderFill_tb."executedQuantity") OVER (PARTITION BY OrderFill_tb."orderBuy"
		ORDER BY
			OrderFill_tb."tradeNo"))
			WHEN OrderFill_tb."tradeNo" IS NOT NULL THEN SUM(OrderFill_tb."executedQuantity") OVER (PARTITION BY OrderFill_tb."orderSell"
		ORDER BY
			OrderFill_tb."tradeNo") - (SUM(OrderFill_tb."executedQuantity") OVER (PARTITION BY OrderFill_tb."orderBuy" ))
			ELSE SUM(OrderFill_tb."executedQuantity") OVER (PARTITION BY OrderFill_tb."orderSell" ) - (SUM(OrderFill_tb."executedQuantity") OVER (PARTITION BY OrderFill_tb."orderBuy" ))
		END
	END
END
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
           CASE
	WHEN OrderSent_tb."requestedQuantity" = OrderFill_tb."executedQuantity" THEN 0
	ELSE
	CASE
		WHEN OrderSent_tb."requestedQuantity" = OrderFill_tb."executedQuantity" THEN 0
		ELSE
		CASE
			WHEN OrderSent_tb."bankDirection" = 'BankBuys' THEN OrderSent_tb."requestedQuantity" -
			CASE
				WHEN OrderFill_tb."tradeNo" IS NOT NULL
				AND OrderFill_tb."tradeNo" IS NOT NULL THEN SUM(OrderFill_tb."executedQuantity") OVER (PARTITION BY OrderFill_tb."orderBuy"
			ORDER BY
				OrderFill_tb."tradeNo") - (SUM(OrderFill_tb."executedQuantity") OVER (PARTITION BY OrderFill_tb."orderSell"
			ORDER BY
				OrderFill_tb."tradeNo"))
				WHEN OrderFill_tb."tradeNo" IS NOT NULL THEN SUM(OrderFill_tb."executedQuantity") OVER (PARTITION BY OrderFill_tb."orderBuy" ) - (SUM(OrderFill_tb."executedQuantity") OVER (PARTITION BY OrderFill_tb."orderSell"
			ORDER BY
				OrderFill_tb."tradeNo"))
				WHEN OrderFill_tb."tradeNo" IS NOT NULL THEN SUM(OrderFill_tb."executedQuantity") OVER (PARTITION BY OrderFill_tb."orderBuy"
			ORDER BY
				OrderFill_tb."tradeNo") - (SUM(OrderFill_tb."executedQuantity") OVER (PARTITION BY OrderFill_tb."orderSell" ))
				ELSE SUM(OrderFill_tb."executedQuantity") OVER (PARTITION BY OrderFill_tb."orderBuy" ) - (SUM(OrderFill_tb."executedQuantity") OVER (PARTITION BY OrderFill_tb."orderSell" ))
			END
			ELSE OrderSent_tb."requestedQuantity" -
			CASE
				WHEN OrderFill_tb."tradeNo" IS NOT NULL
				AND OrderFill_tb."tradeNo" IS NOT NULL THEN SUM(OrderFill_tb."executedQuantity") OVER (PARTITION BY OrderFill_tb."orderSell"
			ORDER BY
				OrderFill_tb."tradeNo") - (SUM(OrderFill_tb."executedQuantity") OVER (PARTITION BY OrderFill_tb."orderBuy"
			ORDER BY
				OrderFill_tb."tradeNo"))
				WHEN OrderFill_tb."tradeNo" IS NOT NULL THEN SUM(OrderFill_tb."executedQuantity") OVER (PARTITION BY OrderFill_tb."orderSell" ) - (SUM(OrderFill_tb."executedQuantity") OVER (PARTITION BY OrderFill_tb."orderBuy"
			ORDER BY
				OrderFill_tb."tradeNo"))
				WHEN OrderFill_tb."tradeNo" IS NOT NULL THEN SUM(OrderFill_tb."executedQuantity") OVER (PARTITION BY OrderFill_tb."orderSell"
			ORDER BY
				OrderFill_tb."tradeNo") - (SUM(OrderFill_tb."executedQuantity") OVER (PARTITION BY OrderFill_tb."orderBuy" ))
				ELSE SUM(OrderFill_tb."executedQuantity") OVER (PARTITION BY OrderFill_tb."orderSell" ) - (SUM(OrderFill_tb."executedQuantity") OVER (PARTITION BY OrderFill_tb."orderBuy" ))
			END
		END
	END
END
         """.toLowerCase().trimIndent().withoutWhitespace()
      )
   }

   @Test
   fun `should generate Sql for when statement referencing a view field on the left hand side of a when clause`() {
      //venueStatus
      val sql = typeUnderTest.toWhenSql(objectType.field("venueStatus"))
      Assert.assertEquals(sql.toString().toLowerCase().trimIndent().withoutWhitespace(),
         """
            case
            	when OrderFill_tb."tradeNo" is not null
            	and OrderSent_tb."requestedQuantity" = OrderFill_tb."executedQuantity" then 'venue1'
            	when OrderSent_tb."requestedQuantity" = SUM(OrderFill_tb."executedQuantity") over (partition by OrderFill_tb."orderBuy" ) then 'venue2'
            	else null
            end
         """.toLowerCase().trimIndent().withoutWhitespace())
   }

   @Test
   fun `and expression with more than two constituents`() {
       val testView  =TestSchemas.fromSchemaSource(TestSchemas.viewWithAndConditionsWithMultipleParts)
       val viewDefinition = testView.taxiView.viewBodyDefinitions!!.first()
       val objectType = (viewDefinition.viewBodyType!! as ObjectType)
       val typeUnderTest = WhenStatementGenerator(
          testView.taxiView,
          objectType,
          testView.tableNameMap,
          testView.taxiSchema)
      val cumQtyFieldSqlExpression = typeUnderTest.toWhenSql(objectType.field("cumQty"))
      Assert.assertEquals(cumQtyFieldSqlExpression.toString().toLowerCase().trimIndent().withoutWhitespace(),
         """
            case
            	when Order_tb."direction" = 'Sell'
            	and Order_tb."marketId" is not null
            	and Order_tb."executedQuantity" is not null
            	and Order_tb."sFlag" = 1 then SUM(Order_tb."executedQuantity") over (partition by Order_tb."direction" )
            	else 0
            end
         """.toLowerCase().trimIndent().withoutWhitespace())
   }

   @Test
   fun `or expression with more than two constituents`() {
      val testView  =TestSchemas.fromSchemaSource(TestSchemas.viewWithAndConditionsWithMultipleParts)
      val viewDefinition = testView.taxiView.viewBodyDefinitions!!.first()
      val objectType = (viewDefinition.viewBodyType!! as ObjectType)
      val typeUnderTest = WhenStatementGenerator(
         testView.taxiView,
         objectType,
         testView.tableNameMap,
         testView.taxiSchema)
      val sellQtySqlExpression = typeUnderTest.toWhenSql(objectType.field("sellQty"))
      Assert.assertEquals(sellQtySqlExpression.toString().toLowerCase().trimIndent().withoutWhitespace(),
         """
            case
               when Order_tb."direction" = 'Sell'
               or Order_tb."marketId" is not null
               or Order_tb."executedQuantity" is not null
               or Order_tb."sFlag" = 1 then SUM(Order_tb."executedQuantity") over (partition by Order_tb."direction" )
               else 0
            end
         """.toLowerCase().trimIndent().withoutWhitespace())
   }

   @Test
   fun `a view with a when with enumm assignment`() {
      val testView  =TestSchemas.fromSchemaSource(TestSchemas.viewWithAWhenStatementUsingEnumAssignments)
      val viewDefinition = testView.taxiView.viewBodyDefinitions!!.first()
      val objectType = (viewDefinition.viewBodyType!! as ObjectType)
      val typeUnderTest = WhenStatementGenerator(
         testView.taxiView,
         objectType,
         testView.tableNameMap,
         testView.taxiSchema)
      val venueOrderStatusSqlExpression = typeUnderTest.toWhenSql(objectType.field("venueOrderStatus"))
      Assert.assertEquals(venueOrderStatusSqlExpression.toString().toLowerCase().trimIndent().withoutWhitespace(),
         """
            CASE WHEN Order_tb."orderQty" = Order_tb."executedQty" THEN 'Filled' ELSE 'Active' END
         """.toLowerCase().trimIndent().withoutWhitespace())
   }

   @Test
   fun `a view with a field substracting two view fields`() {
      val testView  =TestSchemas.fromSchemaSource(TestSchemas.viewWithSubsctraction)
      val viewDefinition = testView.taxiView.viewBodyDefinitions!!.first()
      val objectType = (viewDefinition.viewBodyType!! as ObjectType)
      val typeUnderTest = WhenStatementGenerator(
         testView.taxiView,
         objectType,
         testView.tableNameMap,
         testView.taxiSchema)
      val remainingQuantitySql = typeUnderTest.toWhenSql(objectType.field("remainingQuantity"))
      Assert.assertEquals(remainingQuantitySql.toString().withoutWhitespace().toLowerCase(),
         """
            SUM(Order_tb."reqQty") OVER (PARTITION BY Order_tb."orderId" ORDER BY Order_tb."ts") - (SUM(Order_tb."execQty") OVER (PARTITION BY Order_tb."orderId" ORDER BY Order_tb."ts"))
         """.trimIndent().withoutWhitespace().toLowerCase())
   }
}
