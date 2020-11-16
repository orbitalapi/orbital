package io.vyne

import io.vyne.models.json.parseJsonModel
import io.vyne.query.queryBuilders.VyneQlGrammar
import lang.taxi.services.QueryOperationCapability
import org.junit.Test

class VyneQueryTest {
   @Test
   fun willInvokeAQueryToDiscoverValues() {
      val (vyne, stub) = testVyne("""
         type TraderId inherits String
         model Trade {
            traderId : TraderId
         }
         service TradeService {
            ${queryDeclaration("tradeQuery", "Trade[]")}
         }
      """.trimIndent())

      val response = vyne.parseJsonModel("Trade[]", """[ { "traderId" : "jimmy" } ]""")
      stub.addResponse("tradeQuery", response)
      val queryResult = vyne.query("findAll { Trade[]( TraderId = 'jimmy' ) }")

      TODO()
   }
}

fun queryDeclaration(queryName: String, returnTypeName: String, capabilities: List<QueryOperationCapability> = QueryOperationCapability.ALL): String {
   return """
      vyneQl query $queryName(params:${VyneQlGrammar.QUERY_TYPE_NAME}):$returnTypeName with capabilities {
         ${capabilities.joinToString(", \n") {it.asTaxi()}}
      }
   """.trimIndent()
}
