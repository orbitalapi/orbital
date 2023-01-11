package io.vyne.query

import com.nhaarman.mockito_kotlin.mock
import com.winterbe.expekt.should
import io.vyne.models.json.parseJson
import io.vyne.models.json.parseJsonModel
import io.vyne.queryDeclaration
import io.vyne.rawObjects
import io.vyne.schemas.taxi.TaxiSchema
import io.vyne.testVyne
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import lang.taxi.Compiler
import org.junit.Before
import org.junit.Test
import java.util.UUID

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
      """.trimIndent()
   )

   lateinit var queryOperationStrategy: QueryOperationInvocationStrategy

   @Before
   fun setup() {
      queryOperationStrategy = QueryOperationInvocationStrategy(mock { })
   }

   @Test
   fun matchesQueryOperationForFindAll() {
      val querySpecNode = getQuerySpecNode("find { Person[] }", schema)
      val candidates = queryOperationStrategy.lookForCandidateQueryOperations(schema, querySpecNode)
      candidates.should.have.size(1)
   }

   @Test
   fun matchesQueryOperationFilteringEqualsAttributeName() {
      val querySpecNode = getQuerySpecNode("find { Person[]( FirstName == 'Jimmy' ) }", schema)
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
      """.trimIndent()
      )
      val querySpecNode = getQuerySpecNode("find { Trade[]( TraderName == 'Jimmy' ) }", schema)
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
      """.trimIndent()
      )
      val querySpecNode = getQuerySpecNode("find { Trade[]( TraderName == 'Jimmy' ) }", schema)
      val candidates = queryOperationStrategy.lookForCandidateQueryOperations(schema, querySpecNode)
      candidates.should.have.size(2)
   }


   @Test
   fun invokesRemoteServiceWithCorrectParams() {
      val (vyne, stub) = testVyne(schema)

      stub.addResponse(
         "personQuery", vyne.parseJsonModel(
            "Person[]",
            """
            [ { "firstName" : "Jimmy" } ]
         """.trimIndent()
         )
      )
      val result = runBlocking { vyne.query("find { Person[]( FirstName == 'Jimmy' ) }").results.toList() }

      stub.invocations.should.have.size(1)
      // TODO :  Assert the vyneQl was formed correctly
   }

   @Test
   fun `a query operation is invoked when enriching data`() {
      val (vyne, stub) = testVyne(
         TaxiSchema.fromStrings(
            VyneQlGrammar.QUERY_TYPE_TAXI,
            """
         model Person {
            @Id
            id : PersonId inherits String
            name : PersonName inherits String

         }
         model EmployeeDetails {
            @Id
            id : PersonId inherits String
            managerName : ManagerName inherits String
         }
         service ApiService {
            operation findPeople():Person[]
         }
         service DbService {
            ${queryDeclaration("getEmployeesDetails", "EmployeeDetails[]")}
            ${queryDeclaration("getOneEmployeeDetails", "EmployeeDetails")}
          }
      """.trimIndent()
         )
      )
      stub.addResponse("findPeople", vyne.parseJson("Person[]", """[ { "id" : "001" , "name" :  "Jimmy" } ]"""))
      stub.addResponse(
         "getOneEmployeeDetails",
         vyne.parseJson("EmployeeDetails", """[ { "id" : "001" , "managerName" :  "Jones" } ]""")
      )
      val result = runBlocking {
         vyne.query(
            """find { Person[] } as {
         id : PersonId
         name : PersonName
          managerName : ManagerName
       }[]
      """
         ).rawObjects()
      }

      result.first().should.equal(
         mapOf(
            "id" to "001",
            "name" to "Jimmy",
            "managerName" to "Jones"
         )
      )

   }


}

fun getQuerySpecNode(taxiQl: String, schema: TaxiSchema): QuerySpecTypeNode {
   val (vyne, _) = testVyne(schema)
   val vyneQuery = Compiler(source = taxiQl, importSources = listOf(schema.document)).queries().first()
   val (_, expression) = vyne.buildContextAndExpression(
      vyneQuery,
      queryId = UUID.randomUUID().toString(),
      clientQueryId = null
   )
   val queryParser = QueryParser(schema)
   val querySpecNodes = queryParser.parse(expression)
   return querySpecNodes.first()

}
