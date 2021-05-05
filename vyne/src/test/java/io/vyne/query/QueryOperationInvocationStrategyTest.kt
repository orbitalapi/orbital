package io.vyne.query

import com.nhaarman.mockito_kotlin.mock
import com.winterbe.expekt.should
import io.vyne.models.json.parseJsonModel
import io.vyne.query.queryBuilders.VyneQlGrammar
import io.vyne.queryDeclaration
import io.vyne.schemas.taxi.TaxiSchema
import io.vyne.testVyne
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import lang.taxi.Compiler
import org.junit.Before
import org.junit.Test
import java.util.*

class QueryOperationInvocationStrategyTest {
   val schema = TaxiSchema.fromStrings(
      VyneQlGrammar.QUERY_TYPE_TAXI,
      """
         type FirstName inherits String
         model Person {
            firstName : FirstName
         }
         service PersonService {
            ${queryDeclaration("personQuery", "Person[]")}
         }
      """.trimIndent())

   lateinit var queryOperationStrategy: QueryOperationInvocationStrategy

   @Before
   fun setup() {
      queryOperationStrategy = QueryOperationInvocationStrategy(mock { })
   }

   @Test
   fun matchesQueryOperationForFindAll() {
      val querySpecNode = getQuerySpecNode("findAll { Person[] }", schema)
      val candidates = queryOperationStrategy.lookForCandidateQueryOperations(schema, querySpecNode)
      candidates.should.have.size(1)
   }

   @Test
   fun matchesQueryOperationFilteringEqualsAttributeName() {
      val querySpecNode = getQuerySpecNode("findAll { Person[]( FirstName = 'Jimmy' ) }", schema)
      val candidates = queryOperationStrategy.lookForCandidateQueryOperations(schema, querySpecNode)
      candidates.should.have.size(1)
   }

   @Test
   fun `when querying for base type, services retuning subtype are considered`() {
      val schema = TaxiSchema.fromStrings(
         VyneQlGrammar.QUERY_TYPE_TAXI,
         """
         type TraderName inherits String
         model Trade {}
         model FxTrade inherits Trade {
            trader : TraderName
         }
         model IrsTrade inherits Trade {
            trader : TraderName
         }
         service FxTradeService {
            ${queryDeclaration("fxTradeQuery", "FxTrade[]")}
         }
         service IrsTradeService {
            ${queryDeclaration("irsTradeQuery", "IrsTrade[]")}
         }
      """.trimIndent())
      val querySpecNode = getQuerySpecNode("findAll { Trade[]( TraderName = 'Jimmy' ) }", schema)
      val candidates = queryOperationStrategy.lookForCandidateQueryOperations(schema, querySpecNode)
      candidates.should.have.size(2)
   }

   @Test
   fun `when querying for base type with query params, services retuning subtype but do not contain query param are not considered`() {
      val schema = TaxiSchema.fromStrings(
         VyneQlGrammar.QUERY_TYPE_TAXI,
         """
         type TraderName inherits String
         model Trade {}
         model FxTrade inherits Trade {
            trader : TraderName
         }

         // BondTrade does not expose a TraderName, so in query filtering on TraderName, it should not
         // be invoked
         model BondTrade inherits Trade {
         }
         model IrsTrade inherits Trade {
            trader : TraderName
         }
         service BondTradeService {
            ${queryDeclaration("bondTradeQuery", "BondTrade[]")}
         }
         service FxTradeService {
            ${queryDeclaration("fxTradeQuery", "FxTrade[]")}
         }
         service IrsTradeService {
            ${queryDeclaration("irsTradeQuery", "IrsTrade[]")}
         }
      """.trimIndent())
      val querySpecNode = getQuerySpecNode("findAll { Trade[]( TraderName = 'Jimmy' ) }", schema)
      val candidates = queryOperationStrategy.lookForCandidateQueryOperations(schema, querySpecNode)
      candidates.should.have.size(2)
   }


   @Test
   fun invokesRemoteServiceWithCorrectParams() {
      val (vyne, stub) = testVyne(schema)

      stub.addResponse("personQuery", vyne.parseJsonModel("Person[]",
         """
            [ { "firstName" : "Jimmy" } ]
         """.trimIndent()))
      val result = runBlocking {vyne.query("findAll { Person[]( FirstName = 'Jimmy' ) }").results.toList()}

      stub.invocations.should.have.size(1)
      // TODO :  Assert the vyneQl was formed correctly
   }


}

fun getQuerySpecNode(taxiQl: String, schema: TaxiSchema): QuerySpecTypeNode {
   val (vyne, _) = testVyne(schema)
   val vyneQuery =  Compiler(source = taxiQl, importSources = listOf(schema.document)).queries().first()
   val (_, expression) = vyne.buildContextAndExpression(vyneQuery, queryId = UUID.randomUUID().toString(), clientQueryId = null)
   val queryParser = QueryParser(schema)
   val querySpecNodes = queryParser.parse(expression)
   return querySpecNodes.first()

}
