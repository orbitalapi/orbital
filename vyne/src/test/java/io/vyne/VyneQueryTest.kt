package io.vyne

import com.winterbe.expekt.should
import io.vyne.models.json.parseJsonModel
import io.vyne.query.queryBuilders.VyneQlGrammar
import io.vyne.utils.withoutWhitespace
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import lang.taxi.services.QueryOperationCapability
import org.junit.Test

@ExperimentalCoroutinesApi
class VyneQueryTest {
   @Test
   fun willInvokeAQueryToDiscoverValues() = runBlocking {
      val (vyne, stub) = testVyne(
         VyneQlGrammar.QUERY_TYPE_TAXI,
         """
         type TraderId inherits String
         model Trade {
            traderId : TraderId
         }
         service TradeService {
            ${queryDeclaration("tradeQuery", "Trade[]")}
         }
      """.trimIndent()
      )

      val response = vyne.parseJsonModel("Trade[]", """[ { "traderId" : "jimmy" } ]""")
      stub.addResponse("tradeQuery", response)
      val queryResult = vyne.query("findAll { Trade[]( TraderId = 'jimmy' ) }")

      val resultList = queryResult.rawObjects()
      resultList.should.have.size(1)
      resultList.first()["traderId"].should.equal("jimmy")

      val invocations = stub.invocations["tradeQuery"]!!
      invocations.should.have.size(1)
      val vyneQlQuery = invocations.first().value!! as String

      val expectedVyneQl = """findAll { lang.taxi.Array<Trade>(
          TraderId = "jimmy"
         )
      }"""
      vyneQlQuery.withoutWhitespace()
         .should.equal(expectedVyneQl.withoutWhitespace())

      println(queryResult.results?.toList())
   }

   fun schemaWithOperation(operation: String): String {
      return """model Person {
               id : PersonId inherits String
            }
            service PersonService {
               $operation
            }
            """
   }

   @Test
   fun `when an input is provided in a given, services which do not satisfy contract are not invoked`(): Unit =
      runBlocking {
         val (vyne, stub) = testVyne(schemaWithOperation("""operation findAllPeople():Person[]"""))
         vyne.query("""given { personId : PersonId = "123" } findAll { Person[] }""")
            .results
            .toList()
         stub.invocations.should.be.empty
      }

   @Test
   fun `when an input is in a given, a service which returns the correct type with the input is invoked`(): Unit =
      runBlocking {
         val (vyne, stub) = testVyne(schemaWithOperation("""operation findPerson(PersonId):Person"""))
         vyne.query("""given { personId : PersonId = "123" } findAll { Person }""")
            .results
            .toList()
         stub.invocations.should.be.empty

         // TODO :  This should also be supported, because it's what people 'expect' to happen
         vyne.query("""given { personId : PersonId = "123" } findAll { Person[] }""")
            .results
            .toList()
         stub.invocations.should.be.empty

      }

}

fun queryDeclaration(
   queryName: String,
   returnTypeName: String,
   capabilities: List<QueryOperationCapability> = QueryOperationCapability.ALL
): String {
   return """
      vyneQl query $queryName(params:${VyneQlGrammar.QUERY_TYPE_NAME}):$returnTypeName with capabilities {
         ${capabilities.joinToString(", \n") { it.asTaxi() }}
      }
   """.trimIndent()
}
