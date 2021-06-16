package io.vyne

import com.winterbe.expekt.should
import io.vyne.models.Provided
import io.vyne.models.TypedInstance
import io.vyne.models.json.parseJsonModel
import io.vyne.models.json.parseKeyValuePair
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
    fun willInvokeAQueryToDiscoverValues(): Unit = runBlocking {

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


    }

    @Test
    fun `when a value is returned containing a nested fact, that fact is used in discovery`(): Unit = runBlocking {
        val (vyne, stub) = testVyne(
            """
         type PersonName inherits String
         model PersonIds {
            personId : PersonId inherits String
         }
         model Person {
            identifiers : PersonIds
         }
         service PersonService {
            operation findAllPeople():Person[]
            operation findName(PersonId):PersonName
         }
      """.trimIndent()
        )
        val people = TypedInstance.from(
            vyne.type("Person[]"),
            """[{ "identifiers" : { "personId" : "j123" } }]""",
            vyne.schema,
            source = Provided
        )
        stub.addResponse(
            "findAllPeople",
            people
        )
        stub.addResponse("findName", vyne.parseKeyValuePair("PersonName", "Jimmy"))

        val result = vyne.query(
            """findAll { Person[] } as { id : PersonId
         | name : PersonName }[]""".trimMargin()
        )
            .results.toList()
        result.first().toRawObject().should.equal(
            mapOf("id" to "j123", "name" to "Jimmy")
        )
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
