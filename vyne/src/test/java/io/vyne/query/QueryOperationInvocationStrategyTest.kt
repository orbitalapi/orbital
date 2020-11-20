package io.vyne.query

import com.nhaarman.mockito_kotlin.mock
import com.winterbe.expekt.should
import io.vyne.models.json.parseJsonModel
import io.vyne.query.queryBuilders.VyneQlGrammar
import io.vyne.queryDeclaration
import io.vyne.schemas.taxi.TaxiSchema
import io.vyne.testVyne
import io.vyne.vyneql.VyneQlCompiler
import org.junit.Before
import org.junit.Test

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
   fun invokesRemoteServiceWithCorrectParams() {
      val (vyne, stub) = testVyne(schema)

      stub.addResponse("personQuery", vyne.parseJsonModel("Person[]",
         """
            [ { "firstName" : "Jimmy" } ]
         """.trimIndent()))
      val result = vyne.query("findAll { Person[]( FirstName = 'Jimmy' ) }")
      result.isFullyResolved.should.be.`true`

      stub.invocations.should.have.size(1)
      // TODO :  Assert the vyneQl was formed correctly
   }


}

fun getQuerySpecNode(vyneQl: String, schema: TaxiSchema): QuerySpecTypeNode {
   val (vyne, _) = testVyne(schema)
   val vyneQuery = VyneQlCompiler(vyneQl, schema.taxi).query()
   val (_, expression) = vyne.buildContextAndExpression(vyneQuery)
   val queryParser = QueryParser(schema)
   val querySpecNodes = queryParser.parse(expression)
   return querySpecNodes.first()

}
