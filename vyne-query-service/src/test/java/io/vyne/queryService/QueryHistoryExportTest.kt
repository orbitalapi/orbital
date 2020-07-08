package io.vyne.queryService

import com.fasterxml.jackson.databind.ObjectMapper
import io.vyne.queryService.persistency.ReactiveDatabaseSupport
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

class QueryHistoryExportTest {
   private lateinit var objectMapper: ObjectMapper

   private val orderStr = """
         {
         	"lang.taxi.Array<ion.orders.Order>":[
         		{
         			"qty":0.0,
         			"code":"EZKDW08FH9Y2",
         			"desc":"A-EUR 5Y",
         			"desk":"",
         			"trader":"Ca_Trader6_Ion",
         			"orderID":"496_20200327_0",
         			"orderNo":"496",
         			"verbStr":"Buy",
         			"strategy":"",
         			"accountId":"CALYON.LCH",
         			"entryType":"De",
         			"orderDate":"20200326",
         			"priceType":1,
         			"qtyTotReq":0.0,
         			"priceAmount":-0.23,
         			"timeInForce":7,
         			"currencyCode":"EUR",
         			"instrumentId":"A_EUR_5Y_EURA",
         			"orderTypeStr":"Limit",
         			"priceTypeStr":"Price",
         			"orderDateTime":"20200327 07:37:54",
         			"timeInForceStr":"FAK",
         			"orderExpiryDate":"18991231",
         			"stopPriceAmount":0.0
         		}
         	]
         }
         """.trimIndent()

   @Before
   fun before() {
      val reactiveDatabaseSupport = ReactiveDatabaseSupport()
      reactiveDatabaseSupport.r2dbcCustomConversions()
      objectMapper = reactiveDatabaseSupport.objectMapper
   }

   @Test
   fun exportToCsv() {
      val expected =
         """qty,code,desc,desk,trader,orderID,orderNo,verbStr,strategy,accountId,entryType,orderDate,priceType,qtyTotReq,priceAmount,timeInForce,currencyCode,instrumentId,orderTypeStr,priceTypeStr,orderDateTime,timeInForceStr,orderExpiryDate,stopPriceAmount
0.0,EZKDW08FH9Y2,A-EUR 5Y,,Ca_Trader6_Ion,496_20200327_0,496,Buy,,CALYON.LCH,De,20200326,1,0.0,-0.23,7,EUR,A_EUR_5Y_EURA,Limit,Price,20200327 07:37:54,FAK,18991231,0.0
         """.trimIndent()

      val order = objectMapper.readValue(orderStr, mutableMapOf<String, Any?>()::class.java)
      val historyExporter = QueryHistoryExporter(objectMapper)

      val actual = historyExporter.export(order, ExportType.CSV).toString(Charsets.UTF_8).trimIndent()

      assertEquals(expected, actual)
   }

   @Test
   fun exportToJson() {
      val order = objectMapper.readValue(orderStr, mutableMapOf<String, Any?>()::class.java)
      val historyExporter = QueryHistoryExporter(objectMapper)
      val actual = historyExporter.export(order, ExportType.JSON).toString(Charsets.UTF_8)
      val expected = orderStr.replace("\t", "").replace("\n", "")

      assertEquals(expected, actual)
   }
}
