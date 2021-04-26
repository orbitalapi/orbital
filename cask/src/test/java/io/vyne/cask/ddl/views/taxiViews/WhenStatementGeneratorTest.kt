package io.vyne.cask.ddl.views.taxiViews

import com.winterbe.expekt.should
import io.vyne.cask.ddl.views.trimNewLines
import lang.taxi.types.ObjectType
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
      sql.trimIndent().trimNewLines().should.equal(
         """
            case
            when OrderFill_tb."tradeNo" = null then OrderFill_tb."executedQuantity"
            else SUM (OrderFill_tb."executedQuantity") OVER (PARTITION BY OrderFill_tb."fillOrderId"   ORDER BY OrderFill_tb."tradeNo")
            end
         """.trimIndent().trimNewLines()
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
      sql.trimIndent().trimNewLines().should.equal(
         """
            case
               when OrderSent_tb."requestedQuantity" = OrderFill_tb."executedQuantity" then 0
               else case
                  when OrderFill_tb."tradeNo" = null  then OrderSent_tb."requestedQuantity" - OrderFill_tb."executedQuantity"
                  else OrderSent_tb."requestedQuantity" - SUM (OrderFill_tb."executedQuantity") OVER (PARTITION BY OrderFill_tb."fillOrderId"   ORDER BY OrderFill_tb."tradeNo")
               end
            end
         """.trimIndent().trimNewLines()
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
      sql.trimIndent().trimNewLines().replace(" ", "").should.equal(
         """
            case
               when OrderSent_tb."requestedQuantity" = OrderFill_tb."executedQuantity" then 0
               else
                  case
                     when OrderSent_tb."requestedQuantity" = OrderFill_tb."executedQuantity" then 0
                     else
                        case
                           when OrderFill_tb."tradeNo" = null  then OrderSent_tb."requestedQuantity" - OrderFill_tb."executedQuantity"
                           else OrderSent_tb."requestedQuantity" - SUM (OrderFill_tb."executedQuantity") OVER (PARTITION BY OrderFill_tb."fillOrderId"   ORDER BY OrderFill_tb."tradeNo")
                        end
                  end
            end
         """.trimIndent().trimNewLines().replace(" ", "")
      )

   }

}
