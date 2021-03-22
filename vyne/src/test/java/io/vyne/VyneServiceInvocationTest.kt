package io.vyne

import io.vyne.models.Provided
import io.vyne.models.TypedCollection
import io.vyne.models.TypedInstance
import kotlinx.coroutines.runBlocking
import org.junit.Test
/*
class VyneServiceInvocationTest  {

   @Test
   fun `do not call invalid services`() {
      val (vyne,stub) = testVyne("""
         model Trade {
            @Id
            tradeId : TradeId as String
            traderId : TraderUserId as String
            isin : Isin as String
            settlementDate : SettlementDate as Date
         }
         model Output {
            @FirstNotEmpty
            settlementDate : SettlementDate as Date
         }
         model Product {
            @Id
            isin : Isin
            settlementDate : SettlementDate
         }

         @DataStore
         service TradeService {
            @StubResponse("findAllTrades")
            operation findAllTrades():Trade[]

            // We don't want this to be called.
            @StubResponse("findTradesBySalesPerson")
            operation findTradesBySalesPerson(TraderUserId):Trade
            // We don't want this to be called.
            @StubResponse("findTrade")
            operation findTrade(TradeId):Trade
         }
         service ProductService {
         @StubResponse("findProductData")
            operation findProductData(Isin):Product
         }
      """.trimIndent())
      val tradeJson = """{
         |"tradeId" : "trade1",
         |"traderId" : "jimmy",
         |"isin" : "tradeIsin",
         |"settlementDate" : null
         |}
      """.trimMargin()
      val trade = TypedInstance.from(vyne.type("Trade"), tradeJson,vyne.schema, source = Provided)

      stub.addResponse("findAllTrades", TypedCollection.from(listOf(trade)))
      runBlocking {vyne.query("""findAll { Trade[] } as Output[]""")}

   }
}
*/
